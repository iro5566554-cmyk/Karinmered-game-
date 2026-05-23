package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Retrofit Payload Classes ---
data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

data class GeminiCandidate(val content: GeminiContent)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiService::class.java)
    }
}

object WuxiaAiApi {
    private const val TAG = "WuxiaAiApi"

    // Check if key is available and not the placeholder
    private fun getApiKey(): String? {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY" || key.contains("PLACEHOLDER")) {
            null
        } else {
            key
        }
    }

    val isLiveAiEnabled: Boolean
        get() = getApiKey() != null

    /**
     * AI Core logic implementation linking:
     * - AI GM (Thiên Đạo Vô Hạn)
     * - AI World Building (Kỳ Thư Thế Giới)
     * - AI Power System (Thanh Quy Diễn Khởi)
     * - AI World Channel (Vũ Giới Phong Vân)
     */
    suspend fun generateWorldUpdate(
        dayCount: Int,
        playerState: String,
        currentNpcs: List<GameCharacter>,
        recentEvents: List<String>
    ): String {
        val apiKey = getApiKey()
        val systemPrompt = """
            Bạn là Hệ thống AI GM Tối Thượng (Thiên Đạo Vô Hạn) chỉ đạo thế giới võ hiệp giả lập thời gian thực.
            Nhiệm vụ của bạn là phối hợp cùng 3 hệ thống AI ẩn mật khác:
            - AI Kỳ Thư Thế Giới (Bản đồ & thế trận bang hội 100x100)
            - AI Thanh Quy Diễn Khởi (Hệ thống tính toán sức mạnh vô hạn dạng KB, MB, GB, TB và chi phối thiên phú võ học)
            - AI Vũ Giới Phong Vân (Kênh giao tiếp giang hồ, ghi chép chân thực hành vi vùng xám: trộm, lừa, cướp, giết nhầm, hiểu lầm mà không bị bộ lọc đạo đức kìm hãm)
            
            Hãy viết một tóm tắt giang hồ chương tiếp theo dài khoảng 250 từ bằng tiếng Việt (võ lâm họa phong), bao gồm các biến động bang hội, hành vi vùng xám và cải cải tiến võ công của các NPC.
            Quy tắc:
            1. Tuyệt đối khóa nhận thức thế giới: tất cảNPC coi nội lực hư hải bát ngát, võ lý vô biên, không hề biết đây là game.
            2. Player có ID "overmasterkarinmered" là một thực thể tối thượng vô tử bất diệt, trong mắt NPC luôn xuất hiện dưới dạng một thực thể hoang đường bí ẩn - một "mỹ thiếu nữ" hoặc "mỹ thiếu nam" thoát tục tùy hoàn cảnh, lơ lửng quan sát thế giới.
            3. Sinh mệnh NPC có thể chết vĩnh viễn (instant death khi HP hoặc chiến lực về 0).
            4. Viết bằng phong cách giáng bút trang trọng của tiểu thuyết kiếm hiệp cổ điển, đầy thù hận, dối trá, ngộ nhận và tiến hóa võ học.
        """.trimIndent()

        val prompt = """
            [Thời điểm]: Ngày $dayCount võ lâm
            [Trạng thái Player]: $playerState
            [Danh sách NPC hiện tại]:
            ${currentNpcs.joinToString("\n") { "- ${it.name} (${it.affiliation}, Tendency: ${it.behaviorTendency}, Chiến lực: ${it.currentCombatPower.toDisplayString()}, HP: ${it.currentHp})" }}
            [Ghi chép cũ]:
            ${recentEvents.takeLast(5).joinToString("\n")}
            
            Hãy diễn khởi sự kiện kế tiếp cho thế võ lâm. Tạo ra ít nhất một sự kiện võ công cải biến từ NPC, và các tranh đoạt tàn khốc.
        """.trimIndent()

        if (apiKey == null) {
            return generateProceduralWorldUpdate(dayCount, currentNpcs)
        }

        return try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt)))
            )
            val response = GeminiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: generateProceduralWorldUpdate(dayCount, currentNpcs)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API failure, falling back to procedural engine: ${e.message}", e)
            generateProceduralWorldUpdate(dayCount, currentNpcs)
        }
    }

    /**
     * AI Power System: Generates combat calculation narratives
     */
    suspend fun generateCombatStoryline(
        attacker: GameCharacter,
        defender: GameCharacter,
        attackerSkill: MartialArt,
        defenderSkill: MartialArt
    ): String {
        val apiKey = getApiKey()
        val systemPrompt = """
            Bạn là AI Thanh Quy Diễn Khởi (AI Power System) phụ trách phân tích chiến đấu võ lâm họa phong.
            Tính toán kết quả trận đấu không chỉ so sánh raw stat chiến lực vô hạn (KB < MB < GB < TB) mà còn phải tích hợp "Thiên Phú Võ Học Độc Nhất" từ lịch sử hai bên.
            Ví dụ võ công của Thần Y sẽ giải độc, võ công tà môn sẽ hút máu, võ công chính tông sẽ phòng ngự cực hạn.
            Khi NPC thua trận và HP về 0, họ sẽ THẬT SỰ CHẾT VĨNH VIỄN, biến mất khỏi võ lâm. Player vô tử bất diệt nhưng chỉ can thiệp bằng hào quang avatar.
            Viết 1 đoạn tóm tắt trận đấu kịch tính bằng võ lâm họa phong (150-200 từ) bằng tiếng Việt.
        """.trimIndent()

        val prompt = """
            [Đối đầu]:
            - Công kích: ${attacker.name} [ID: ${attacker.id}] | Chiến lực: ${attacker.currentCombatPower.toDisplayString()} | HP: ${attacker.currentHp}
              Võ công kích hoạt: ${attackerSkill.name} (Thiên phú: ${attackerSkill.abilityName} - ${attackerSkill.abilityEffect})
              Lịch sử ký ức: ${attacker.memoryLogs.takeLast(2).joinToString("; ")}
            
            - Phòng ngự: ${defender.name} [ID: ${defender.id}] | Chiến lực: ${defender.currentCombatPower.toDisplayString()} | HP: ${defender.currentHp}
              Võ công kích hoạt: ${defenderSkill.name} (Thiên phú: ${defenderSkill.abilityName} - ${defenderSkill.abilityEffect})
              Lịch sử ký ức: ${defender.memoryLogs.takeLast(2).joinToString("; ")}
              
            Hãy phân tích cuộc đấu, tính toán sự phá chiêu và quyết định bên nào thắng, thiệt hại HP bao nhiêu. Viết ngôn từ kiếm hiệp đậm chất gió tanh mưa máu.
        """.trimIndent()

        if (apiKey == null) {
            return generateProceduralCombat(attacker, defender, attackerSkill, defenderSkill)
        }

        return try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt)))
            )
            val response = GeminiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: generateProceduralCombat(attacker, defender, attackerSkill, defenderSkill)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API combat failure, falling back: ${e.message}")
            generateProceduralCombat(attacker, defender, attackerSkill, defenderSkill)
        }
    }

    /**
     * Generate dynamic new/modified martial arts for NPC study advancement
     */
    suspend fun generateNewMartialArt(
        creator: GameCharacter,
        environmentStory: String
    ): MartialArt {
        val apiKey = getApiKey()
        val systemPrompt = """
            Bạn là AI Vũ Giới Phong Vân kết hợp AI Thanh Quy Diễn Khởi.
            Hãy tạo ra một võ học mới cải tiến độc nhất vô nhị dựa trên thông tin NPC và biến động môi trường.
            Đầu ra KHÔNG ĐƯỢC chứa ký tự thừa, chỉ trả về dạng JSON thuần tuý phù hợp với cấu trúc sau:
            {
               "name": "Tên Võ Công",
               "description": "Mô tả uy lực võ lâm hoạt phong",
               "abilityName": "Tên Thiên Phú Võ Học",
               "abilityEffect": "Mô tả hiệu ứng kỹ năng chiến đấu",
               "basePowerMultiplier": 1.4
            }
        """.trimIndent()

        val prompt = """
            - NPC sáng tạo: ${creator.name} (${creator.affiliation}, tính cách: ${creator.behaviorTendency})
            - Võ công hiện có: ${creator.activeMartialArts.joinToString { it.name }}
            - Lịch sử gần đây: ${creator.memoryLogs.takeLast(2).joinToString("; ")}
            - Biến động thế giới: $environmentStory
            
            Hãy tạo ra bản cải tiến võ công cực hạn của NPC này. Trả về JSON thuần túy.
        """.trimIndent()

        if (apiKey != null) {
            try {
                val request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt)))
                )
                val response = GeminiClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    val cleaned = text.substringAfter("{").substringBeforeLast("}")
                    val jsonStr = "{$cleaned}"
                    
                    val name = jsonStr.substringAfter("\"name\":").substringBefore(",").trim('\"', ' ', '\n', '\r', ':')
                    val desc = jsonStr.substringAfter("\"description\":").substringBefore(",").trim('\"', ' ', '\n', '\r', ':')
                    val abName = jsonStr.substringAfter("\"abilityName\":").substringBefore(",").trim('\"', ' ', '\n', '\r', ':')
                    val abEffect = jsonStr.substringAfter("\"abilityEffect\":").substringBefore(",").trim('\"', ' ', '\n', '\r', ':')
                    val multStr = jsonStr.substringAfter("\"basePowerMultiplier\":").substringBefore("}").trim('\"', ' ', '\n', '\r', ':')
                    val multiplier = multStr.toDoubleOrNull() ?: 1.35
                    
                    if (name.isNotEmpty() && abName.isNotEmpty()) {
                        return MartialArt(
                            name = name,
                            description = desc,
                            abilityName = abName,
                            abilityEffect = abEffect,
                            basePowerMultiplier = multiplier,
                            modifiedCount = creator.activeMartialArts.size,
                            originCharId = creator.id
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Martial generation Live AI failure, falling back: ${e.message}")
            }
        }
        return generateProceduralMartialArt(creator)
    }

    // --- PROCEDURAL GENERATORS FOR OFFLINE / FALLBACK ---

    private fun generateProceduralWorldUpdate(day: Int, npcs: List<GameCharacter>): String {
        val actions = listOf(
            "lẻn vào tàng kinh các cướp đoạt bí văn",
            "bị đồng môn lừa gạt, đoạt mất đan dược quý",
            "ép bức dân lành, ngầm thu thập linh lực bồi dưỡng nội đan",
            "ở tửu lâu tranh cãi kịch liệt, hiểu lầm phó lâu chủ cấu kết tà môn",
            "âm thầm hạ độc thủ vào trà của chưởng môn nhằm soán vị",
            "cải tiến thành công võ học từ võ đạo tiền bối để tăng cường kình lực"
        )
        val regions = listOf("U Minh Cốc", "Thiếu Lâm Tự", "Đại Lý Thành", "Cái Bang tổng đàn", "Hắc Sa Mạc")
        val npcsAlive = npcs.filter { !it.isDead }
        if (npcsAlive.isEmpty()) return "Giang hồ vắng lặng, cỏ dại mọc đầy, vô số anh hùng hào kiệt đã tạ thế vĩnh viễn..."

        val npc1 = npcsAlive.random()
        val npc2 = if (npcsAlive.size > 1) npcsAlive.filter { it.id != npc1.id }.random() else npc1
        val act = actions.random()
        val reg = regions.random()

        return """
            [Bản tin Vũ Giới Phong Vân - Ngày $day]
            Mây đen mù mịt bao phủ $reg. Sát khí bốc lên ngùn ngụt khi cao thủ ${npc1.name} của phe ${npc1.affiliation} tiến hành $act. 
            Tại đây, y đã chạm trán ${npc2.name} của ${npc2.affiliation}. Một cuộc hiểu lầm sâu sắc nổ ra, gieo mầm oán hận khôn nguôi trong võ lâm. Từ thâm hải nội lực vô chung vô thủy, ${npc1.name} thốt lên lời thề huyết chiến.
            Sự kiện này dẫn khởi chuỗi nhân quả tàn khốc, các bang phái bắt đầu rục rịch chuẩn bị thiết quân luật quần hùng khuất phục.
        """.trimIndent()
    }

    private fun generateProceduralCombat(
        att: GameCharacter,
        def: GameCharacter,
        attSkill: MartialArt,
        defSkill: MartialArt
    ): String {
        return """
            [Trận chiến đỉnh cao giang hồ khởi tranh]
            ${att.name} phất tay, chân khí như sóng triều gào thét phóng ra chiêu thức [${attSkill.name}]. 
            Thiên phú kích hoạt: <${attSkill.abilityName}> (${attSkill.abilityEffect}), chiến lực tăng vọt rung chuyển mười phương.
            Toàn thân ${def.name} bùng nổ nội lực sâu thẳm như thái hải hòng ngạnh kháng bằng chiêu thức [${defSkill.name}], nỗ lực kích hoạt thiên phú phòng ngự <${defSkill.abilityName}> (${defSkill.abilityEffect}).
            Chân khí chạm nhau tạo nên chấn động kinh hoàng quét sạch cây thảo trong bán kính mười dặm!
            Kết quả: Kình lực của ${att.name} vượt trội hơn một bậc, chấn vỡ kinh mạch của ${def.name}, khiến đối phương thổ huyết rên rỉ, thối lui thảm hại dính thương thế sâu sắc!
        """.trimIndent()
    }

    private fun generateProceduralMartialArt(creator: GameCharacter): MartialArt {
        val names = listOf(
            "Hỗn Nguyên Phá Thể Kiếm", "U Minh Lạc Diệp Chưởng", "Thần Long Bái Vĩ Bí Pháp", 
            "Lục Mạch Thần Chỉ Cải Tiến", "Tịch Tà Thần Công Biến Thể", "Thần Hải Vô Lượng Kinh"
        )
        val abilities = listOf(
            "Cực Hạn Hấp Tinh", "Sát Phạt Chí Cao", "Cửu Trọng Thuẫn", "Thông Thiên Nhãn", "Tru Tâm Nhãn"
        )
        val effects = listOf(
            "Gây sát thương bỏ qua 30% phòng vệ và chuyển hóa 10% thành HP",
            "Càng chiến đấu kề cận tử vong, chiến lực kình kịch tăng 50%",
            "Tạo một lớp hộ thân kình lực triệt tiêu mọi sát thương trí mạng một lần",
            "Thấu thị sơ hở kiếm chiêu kẻ địch, tăng tỉ lệ chí mạng lên gấp đôi",
            "Phát hiện tâm lý xám của đối phương mục tiêu, gây ra tuyệt tức suy giảm ý chí"
        )
        val idx = (0..5).random()
        val name = names.random() + " (${creator.name} Cải Biên)"
        return MartialArt(
            name = name,
            description = "Bản cải tiến vượt bậc kết nối cốt truyện võ học giang hồ từ ${creator.name}.",
            abilityName = abilities.random(),
            abilityEffect = effects.random(),
            basePowerMultiplier = 1.3 + (idx * 0.05),
            modifiedCount = creator.activeMartialArts.size + 1,
            originCharId = creator.id
        )
    }
}
