/*
 * Copyright (C) 2024 AI Chat Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.tts

import android.util.Log
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin SentencePiece unigram tokenizer.
 * Reads the binary .model protobuf and performs BPE/Unigram tokenization.
 *
 * The SentencePiece .model file is a protobuf with:
 *   - Field 1 (TrainerSpec): training config (we skip)
 *   - Field 2 (NormalizerSpec): normalization config (we skip)
 *   - Field 3 repeated (SentencePiece): vocab pieces
 *     - Field 1: piece string
 *     - Field 2: score float
 *     - Field 3: type enum (1=NORMAL, 2=UNKNOWN, 3=CONTROL, 4=USER_DEFINED, 6=BYTE)
 */
class SentencePieceTokenizer private constructor(
    private val pieces: List<VocabPiece>,
    private val pieceToId: Map<String, Int>,
    private val unkId: Int,
    private val bosId: Int,
    private val eosId: Int,
) {

    data class VocabPiece(
        val piece: String,
        val score: Float,
        val type: Int, // 1=NORMAL, 2=UNKNOWN, 3=CONTROL, 6=BYTE
    )

    companion object {
        private const val TAG = "SPTokenizer"
        private const val SPIECE_UNDERLINE = '\u2581' // ▁

        fun load(modelFile: File): SentencePieceTokenizer {
            return load(modelFile.inputStream())
        }

        fun load(stream: InputStream): SentencePieceTokenizer {
            val bytes = stream.use { it.readBytes() }
            val pieces = mutableListOf<VocabPiece>()

            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            var pos = 0

            while (pos < bytes.size) {
                val (fieldNum, wireType, newPos) = readTag(bytes, pos)
                pos = newPos

                when {
                    // Field 3 = SentencePiece entries (length-delimited)
                    fieldNum == 1 && wireType == 2 -> {
                        val (len, dataPos) = readVarint(bytes, pos)
                        pos = dataPos + len.toInt()
                    }
                    fieldNum == 2 && wireType == 2 -> {
                        val (len, dataPos) = readVarint(bytes, pos)
                        pos = dataPos + len.toInt()
                    }
                    fieldNum == 3 && wireType == 2 -> {
                        val (len, dataPos) = readVarint(bytes, pos)
                        val pieceBytes = bytes.copyOfRange(dataPos, dataPos + len.toInt())
                        pos = dataPos + len.toInt()
                        val piece = parseSentencePiece(pieceBytes)
                        pieces.add(piece)
                    }
                    wireType == 0 -> {
                        val (_, newP) = readVarint(bytes, pos)
                        pos = newP
                    }
                    wireType == 1 -> {
                        pos += 8
                    }
                    wireType == 2 -> {
                        val (len, dataPos) = readVarint(bytes, pos)
                        pos = dataPos + len.toInt()
                    }
                    wireType == 5 -> {
                        pos += 4
                    }
                    else -> {
                        pos = bytes.size // stop
                    }
                }
            }

            val pieceToId = HashMap<String, Int>(pieces.size)
            var unkId = 0
            var bosId = 1
            var eosId = 2

            for ((i, p) in pieces.withIndex()) {
                pieceToId[p.piece] = i
                when (p.piece) {
                    "<unk>" -> unkId = i
                    "<s>" -> bosId = i
                    "</s>" -> eosId = i
                }
            }

            Log.d(TAG, "Loaded ${pieces.size} pieces (unk=$unkId, bos=$bosId, eos=$eosId)")
            return SentencePieceTokenizer(pieces, pieceToId, unkId, bosId, eosId)
        }

        private fun parseSentencePiece(data: ByteArray): VocabPiece {
            var piece = ""
            var score = 0.0f
            var type = 1 // NORMAL

            var pos = 0
            while (pos < data.size) {
                val (fieldNum, wireType, newPos) = readTag(data, pos)
                pos = newPos

                when {
                    fieldNum == 1 && wireType == 2 -> {
                        val (len, dataPos) = readVarint(data, pos)
                        piece = String(data, dataPos, len.toInt(), Charsets.UTF_8)
                        pos = dataPos + len.toInt()
                    }
                    fieldNum == 2 && wireType == 5 -> {
                        score = ByteBuffer.wrap(data, pos, 4)
                            .order(ByteOrder.LITTLE_ENDIAN).float
                        pos += 4
                    }
                    fieldNum == 3 && wireType == 0 -> {
                        val (v, newP) = readVarint(data, pos)
                        type = v.toInt()
                        pos = newP
                    }
                    wireType == 0 -> {
                        val (_, newP) = readVarint(data, pos)
                        pos = newP
                    }
                    wireType == 1 -> pos += 8
                    wireType == 2 -> {
                        val (len, dataPos) = readVarint(data, pos)
                        pos = dataPos + len.toInt()
                    }
                    wireType == 5 -> pos += 4
                    else -> pos = data.size
                }
            }

            return VocabPiece(piece, score, type)
        }

        private fun readTag(data: ByteArray, pos: Int): Triple<Int, Int, Int> {
            val (value, newPos) = readVarint(data, pos)
            val fieldNum = (value shr 3).toInt()
            val wireType = (value and 0x7).toInt()
            return Triple(fieldNum, wireType, newPos)
        }

        private fun readVarint(data: ByteArray, startPos: Int): Pair<Long, Int> {
            var result = 0L
            var shift = 0
            var pos = startPos
            while (pos < data.size) {
                val b = data[pos].toInt() and 0xFF
                result = result or ((b.toLong() and 0x7F) shl shift)
                pos++
                if (b and 0x80 == 0) break
                shift += 7
            }
            return Pair(result, pos)
        }
    }

    fun getBosId(): Int = bosId
    fun getEosId(): Int = eosId
    fun getUnkId(): Int = unkId
    fun vocabSize(): Int = pieces.size

    /**
     * Encode text to token IDs using unigram (Viterbi) algorithm.
     */
    fun encode(text: String): IntArray {
        if (text.isEmpty()) return intArrayOf()

        // SentencePiece normalizes by prepending ▁ and replacing spaces with ▁
        val normalized = SPIECE_UNDERLINE + text.replace(' ', SPIECE_UNDERLINE)

        return encodeUnigram(normalized)
    }

    /**
     * Viterbi-based unigram tokenization.
     * Finds the highest-score segmentation of the input string.
     */
    private fun encodeUnigram(text: String): IntArray {
        val n = text.length

        // best[i] = (best_score_to_reach_position_i, best_piece_length)
        val bestScore = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val bestLen = IntArray(n + 1)
        bestScore[0] = 0.0f

        for (i in 0 until n) {
            if (bestScore[i] == Float.NEGATIVE_INFINITY) continue

            // Try all substrings starting at position i
            val maxPieceLen = minOf(n - i, 64) // SentencePiece max piece length
            for (len in 1..maxPieceLen) {
                val substr = text.substring(i, i + len)
                val id = pieceToId[substr]
                if (id != null) {
                    val piece = pieces[id]
                    if (piece.type == 1 || piece.type == 4) { // NORMAL or USER_DEFINED
                        val score = bestScore[i] + piece.score
                        if (score > bestScore[i + len]) {
                            bestScore[i + len] = score
                            bestLen[i + len] = len
                        }
                    }
                }
            }

            // Fallback: single byte piece for unknown characters
            if (bestScore[i + 1] == Float.NEGATIVE_INFINITY) {
                val byteStr = byteToHexPiece(text[i])
                val id = pieceToId[byteStr]
                val score = bestScore[i] + (if (id != null) pieces[id].score else -100.0f)
                if (score > bestScore[i + 1]) {
                    bestScore[i + 1] = score
                    bestLen[i + 1] = 1
                }
            }
        }

        // Backtrack to find pieces
        val tokenIds = mutableListOf<Int>()
        var pos = n
        while (pos > 0) {
            val len = bestLen[pos]
            if (len == 0) {
                // Unreachable character — use unk
                tokenIds.add(unkId)
                pos--
            } else {
                val substr = text.substring(pos - len, pos)
                val id = pieceToId[substr]
                if (id != null) {
                    tokenIds.add(id)
                } else {
                    // Try byte fallback
                    val byteId = pieceToId[byteToHexPiece(text[pos - 1])]
                    tokenIds.add(byteId ?: unkId)
                }
                pos -= len
            }
        }

        tokenIds.reverse()
        return tokenIds.toIntArray()
    }

    private fun byteToHexPiece(c: Char): String {
        val bytes = c.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size == 1) {
            return "<0x${String.format("%02X", bytes[0].toInt() and 0xFF)}>"
        }
        return "<0x${String.format("%02X", bytes[0].toInt() and 0xFF)}>"
    }

    fun decode(ids: IntArray): String {
        val sb = StringBuilder()
        for (id in ids) {
            if (id in pieces.indices) {
                val piece = pieces[id]
                if (piece.type == 3) continue // skip CONTROL tokens
                sb.append(piece.piece)
            }
        }
        return sb.toString()
            .replace(SPIECE_UNDERLINE, ' ')
            .trimStart()
    }
}
