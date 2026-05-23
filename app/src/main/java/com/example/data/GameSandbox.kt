package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class GameSandbox(private val context: Context) {
    private val TAG = "GameSandbox"
    private val prefs = context.getSharedPreferences("vo_han_vo_lam_prefs", Context.MODE_PRIVATE)
    
    // --- MOSHI SETUP FOR STATE SERIALIZATION ---
    private val moshi = Moshi.Builder().build()
    
    // Core game state observables
    private val _player = MutableStateFlow<GameCharacter>(createDefaultPlayer())
    val player = _player.asStateFlow()

    private val _npcs = MutableStateFlow<List<GameCharacter>>(emptyList())
    val npcs = _npcs.asStateFlow()

    private val _events = MutableStateFlow<List<WorldEvent>>(emptyList())
    val events = _events.asStateFlow()

    private val _simulatedDay = MutableStateFlow(1)
    val simulatedDay = _simulatedDay.asStateFlow()

    private val _simulatedHour = MutableStateFlow(0) // 0 to 23 hours
    val simulatedHour = _simulatedHour.asStateFlow()

    private val _sectors = MutableStateFlow<List<RegionSector>>(emptyList())
    val sectors = _sectors.asStateFlow()

    private val _isTicking = MutableStateFlow(false)
    val isTicking = _isTicking.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    init {
        setupWorldSectors()
        loadGame()
        if (_npcs.value.isEmpty()) {
            resetWorld()
        }
    }

    private fun createDefaultPlayer(): GameCharacter {
        return GameCharacter(
            id = "overmasterkarinmered", // LOCKED ID
            name = "Vong Ưu Linh Thể",
            isPlayer = true,
            gender = "Mỹ Thiếu Nữ", // Default representation inside NPC mindscape
            basePower = CombatPower(120.0, 1), // Starts at 120 KB
            currentHp = 100.0, // Cannot die
            activeMartialArts = listOf(
                MartialArt(
                    name = "Vong Tình Lục (Thiên Phú Khởi Bản)",
                    description = "Kiếm tâm đoạn tình tuyệt ái, thấu đạt ý thức tối thượng từ trên vạn chúng vô biên.",
                    abilityName = "Chân Thần Bất Diệt",
                    abilityEffect = "Phong tỏa hoàn toàn tử vong, player tuyệt đối không thể chết, trường sinh tự do.",
                    basePowerMultiplier = 1.0,
                    modifiedCount = 0
                )
            ),
            position = WorldPosition(50, 50),
            memoryLogs = listOf("Thần thể rơi xuống phàm trần tại Vô Danh Trấn cô tịch, quan sát võ lâm thái hải."),
            affiliation = "Thiên Nhân Chi Đạo",
            behaviorTendency = "Thần Cách"
        )
    }

    private fun setupWorldSectors() {
        _sectors.value = listOf(
            RegionSector(0, 0, "Băng Cực Cốc", "Tuyết Sơn hoang vu", "Hỗn Loạn Kỳ Bí", "Vùng cực bắc tuyết phong lạnh buốt, thích hạp các đại ma môn phục kích lẩn trốn."),
            RegionSector(0, 40, "Thiếu Lâm Tự", "Môn Phái cổ kính", "Trật Tự Nghiêm Ngặt", "Thánh địa võ học Phật môn tông tự phái, đầy rẫy cao tăng chấn giữ vạn kình tôn kinh."),
            RegionSector(40, 0, "Tiêu Dao Am", "Cổ Trầm Nhã Cốc", "Trật Tự", "Ẩn sơn di tích phái tự phóng khoáng, võ học vô thực vô hư khinh bỉ thế tục."),
            RegionSector(40, 40, "Dã Ngoại Vùng Xám", "Thảo Nguyên bát ngát", "Vô Quy Tắc", "Giang hồ đại lộ giao tranh tàn khốc, các hành vi giật đồ, cướp võ, đánh trộm đầy rẫy nhân quả."),
            RegionSector(70, 0, "Ác Nhân Cốc", "Quỷ Đạo Thạch Lâm", "Hỗn Loạn Cực Độ", "Cực ác hội tụ đầy trộm lừa hung tàn, tà phái cao thủ tự nghiên cứu thay đổi võ môn ác liệt."),
            RegionSector(70, 70, "Đại Lý Sa Mạc", "Hoang Mạc chước nhiệt", "Nguy Hiểm Cực Hạn", "Bão cát mịt mờ hoang địa vô cực, chứa đựng tàn tích võ công thiên cổ bí ẩn.")
        )
    }

    fun getSectorAt(pos: WorldPosition): RegionSector {
        // Find nearest sector base
        return _sectors.value.minByOrNull {
            val sX = it.xStart + 15
            val sY = it.yStart + 15
            kotlin.math.abs(pos.x - sX) + kotlin.math.abs(pos.y - sY)
        } ?: _sectors.value.first()
    }

    fun setPlayerGender(gender: String) {
        _player.value = _player.value.copy(gender = gender)
        saveGame()
    }

    fun setPlayerName(name: String) {
        _player.value = _player.value.copy(name = name)
        saveGame()
    }

    fun teleportPlayer(pos: WorldPosition) {
        if (pos.x in 0..100 && pos.y in 0..100) {
            _player.value = _player.value.copy(position = pos)
            logSystemEvent("Vong kình thiên thần đột ngột biến ảo, hạ phàm tại tọa độ (${pos.x}, ${pos.y}) trong sự ngỡ ngàng của thiên địa.")
            saveGame()
        }
    }

    fun updateTickingState(ticking: Boolean) {
        _isTicking.value = ticking
    }

    /**
     * Resets the entire cosmos of Vietnamese AI Wuxia simulation
     */
    fun resetWorld() {
        val names = listOf(
            "Độc Cô Thất Dạ", "Lục Vô Song", "Chu Thanh Vân", "Tiêu Phong Huyết", "Mộ Dung Tấn",
            "Mỗi Thiên Hàn", "Diệp Cô Phong", "Tạ Thanh Tuyết", "Khưu Xứ Cơ", "Bạch Ngự Phong",
            "Công Tôn Diễm", "Tư Ma Ý", "Xà Thiên Độc", "Chấn Sơn Hùng", "Vô Ảnh Đạo Nhân"
        )
        val affiliations = listOf("Chính Đạo聯盟", "Thiếu Lâm", "Hắc Đạo Liên Minh", "Thần Long Hội", "Tiêu Dao Phái", "Phái Cái Bang", "Ác Nhân Cốc")
        val tendencies = listOf("Righteous", "Evil", "Neutral", "Gray")
        
        // Randomly generate initial NPCs with various power units (KB to MB to GB)
        val initialNpcs = (1..18).map { i ->
            val name = names[i % names.size] + " ${10 + i}"
            val aff = affiliations[Random.nextInt(affiliations.size)]
            val tend = tendencies[Random.nextInt(tendencies.size)]
            
            // Random power layout
            val exp = if (Random.nextFloat() < 0.3) 2 else 1 // 30% start with MB, 70% with KB
            val basePow = Random.nextDouble(100.0, 950.0)
            
            val initialMartial = when (i % 3) {
                0 -> MartialArt(
                    name = "Nhất Dương Chỉ Kỳ Thư",
                    description = "Cương dương chỉ lực thấu đạt thiên địa kinh mạch cao siêu.",
                    abilityName = "Dương Chỉ Khí Hóa",
                    abilityEffect = "Phá vỡ hộ thân kình lực đối thủ, bỏ qua phòng ngự của kẻ tà đạo.",
                    basePowerMultiplier = 1.25,
                    modifiedCount = 0
                )
                1 -> MartialArt(
                    name = "Tà Kinh Hấp Kình Pháp",
                    description = "Hấp thụ nội ma kình khí của quần phong diệt địch nhân.",
                    abilityName = "Phệ Kình Phục Thể",
                    abilityEffect = "Tự động hồi 15% HP sau khi gây thương thế trực tiếp.",
                    basePowerMultiplier = 1.3,
                    modifiedCount = 0
                )
                else -> MartialArt(
                    name = "U Minh Phong Vân Chưởng",
                    description = "Chưởng phong tà đạo u u bách mị như phong vũ tấp nập.",
                    abilityName = "Ảo Linh Ảnh",
                    abilityEffect = "Làm phân tán thần trí đối thủ, tăng cao tột cùng khả năng né tránh kình chí mạng.",
                    basePowerMultiplier = 1.2,
                    modifiedCount = 0
                )
            }

            val rX = Random.nextInt(5, 95)
            val rY = Random.nextInt(5, 95)

            GameCharacter(
                id = "npc_${System.currentTimeMillis()}_$i",
                name = name,
                isPlayer = false,
                gender = if (Random.nextBoolean()) "Nam nhân" else "Nữ hiệp",
                basePower = CombatPower(basePow, exp),
                currentHp = 100.0,
                activeMartialArts = listOf(initialMartial),
                position = WorldPosition(rX, rY),
                memoryLogs = listOf("Thần lực thức tỉnh khởi nguyên trong nhân gian, thuộc phái $aff. Bắt đầu tầm đạo giang hồ."),
                affiliation = aff,
                behaviorTendency = tend,
                isDead = false
            )
        }

        _player.value = createDefaultPlayer()
        _npcs.value = initialNpcs
        _simulatedDay.value = 1
        _simulatedHour.value = 0
        
        _events.value = listOf(
            WorldEvent(
                timestamp = "Ngày 1, Giờ Tý",
                content = "Cơ sở thiên địa đại địa võ lâm được tái thiết phóng khởi vô hạn. Khởi nguồn 100x100 kết cấu bản đồ phân chia môn phái sẵn sàng diễn sinh vạn quỷ tàn tạ. Tát cả NPC và Player 'overmasterkarinmered' đã sẵn sàng can thiệp cuộc đời dối trá.",
                relatedX = 50,
                relatedY = 50,
                sectorName = "Vô Danh Trấn"
            )
        )
        logSystemEvent("Vũ trụ võ học vô tận được khởi sinh lại từ đầu. Thiên Đạo Vô Hạn lập tức lập trình lock logic các AI ẩn.")
        saveGame()
    }

    /**
     * Executes ONE hour / tick in real-time.
     */
    suspend fun executeGameTick() {
        if (_isBusy.value) return
        _isBusy.value = true

        try {
            var currentHour = _simulatedHour.value + 1
            var currentDay = _simulatedDay.value
            if (currentHour >= 24) {
                currentHour = 0
                currentDay += 1
                _simulatedDay.value = currentDay
                // Daily simulated full evaluation
                runDailyUpdateCycle(currentDay)
            }
            _simulatedHour.value = currentHour

            // Move NPCs randomly
            moveNpcsAndCheckCombats(currentDay, currentHour)

        } catch (e: Exception) {
            Log.e(TAG, "Error in game tick: ${e.message}")
        } finally {
            _isBusy.value = false
            saveGame()
        }
    }

    /**
     * Moves NPCs randomly and checks if any encounter occurs on the 100x100 grid.
     */
    private suspend fun moveNpcsAndCheckCombats(day: Int, hour: Int) {
        val currentList = _npcs.value.toMutableList()
        val timeString = "Ngày $day, Giờ ${getFormattedHour(hour)}"

        for (i in currentList.indices) {
            val npc = currentList[i]
            if (npc.isDead) continue

            // Random move +/- 1, 2 tiles
            val dX = Random.nextInt(-3, 4)
            val dY = Random.nextInt(-3, 4)
            var nX = (npc.position.x + dX).coerceIn(1, 99)
            var nY = (npc.position.y + dY).coerceIn(1, 99)

            currentList[i] = npc.copy(position = WorldPosition(nX, nY))
        }

        // Check if any NPCs bump into each other for combat (within distance of 2)
        val aliveNpcs = currentList.filter { !it.isDead }
        var combatTriggered = false

        for (i in aliveNpcs.indices) {
            if (combatTriggered) break
            val npc1 = aliveNpcs[i]

            for (j in (i + 1) until aliveNpcs.size) {
                val npc2 = aliveNpcs[j]
                if (npc1.position.distanceTo(npc2.position) <= 3) {
                    // Encounter triggers due to moral clash or pure grey behavior
                    val sector = getSectorAt(npc1.position)
                    
                    // Trigger Live or fallback AI combat story
                    val skill1 = npc1.activeMartialArts[Random.nextInt(npc1.activeMartialArts.size)]
                    val skill2 = npc2.activeMartialArts[Random.nextInt(npc2.activeMartialArts.size)]

                    logSystemEvent("Phát hiện xung đột kịch liệt võ đạo tại tọa độ (${npc1.position.x}, ${npc1.position.y}) rìa ${sector.name} giữa ${npc1.name} và ${npc2.name}!")
                    
                    val story = WuxiaAiApi.generateCombatStoryline(npc1, npc2, skill1, skill2)

                    // Compute damage based on Power levels
                    val multRatio = getCombatPowerRatioMultiplier(npc1.currentCombatPower, npc2.currentCombatPower)
                    val damageTo1 = (Random.nextDouble(15.0, 35.0) * (2.0 - multRatio)).coerceIn(5.0, 100.0)
                    val damageTo2 = (Random.nextDouble(15.0, 35.0) * multRatio).coerceIn(5.0, 100.0)

                    val newHp1 = (npc1.currentHp - damageTo1).coerceAtLeast(0.0)
                    val newHp2 = (npc2.currentHp - damageTo2).coerceAtLeast(0.0)

                    // Write updates
                    val idx1 = currentList.indexOfFirst { it.id == npc1.id }
                    if (idx1 != -1) {
                        val memory = currentList[idx1].memoryLogs.toMutableList()
                        memory.add("Tranh đấu sinh tử với ${npc2.name} tổn hại ${damageTo1.toInt()}% nguyên kình.")
                        val isNpc1Dead = newHp1 <= 0.0
                        if (isNpc1Dead) {
                            memory.add("ĐỘT TỬ VĨNH VIỄN: HP triệt tiêu biến thành cát bụi tại hoang địa.")
                            logSystemEvent("Võ lâm điêu linh! Cao thủ ${npc1.name} đã ngã xuống vĩnh viễn (Instant Death) tại ${sector.name}!")
                        }
                        currentList[idx1] = currentList[idx1].copy(
                            currentHp = newHp1,
                            isDead = isNpc1Dead,
                            memoryLogs = memory
                        )
                    }

                    val idx2 = currentList.indexOfFirst { it.id == npc2.id }
                    if (idx2 != -1) {
                        val memory = currentList[idx2].memoryLogs.toMutableList()
                        memory.add("Tử chiến huyết đấu với ${npc1.name} tàn hại ${damageTo2.toInt()}% nguyên khí thượng thặng.")
                        val isNpc2Dead = newHp2 <= 0.0
                        if (isNpc2Dead) {
                            memory.add("TỬ VONG: Kinh mạch đứt lìa, danh tính bị xoá sạch khỏi trần ai.")
                            logSystemEvent("Hủy diệt vĩnh hằng! NPC danh gia ${npc2.name} vỡ nát đan điền tử vong vĩnh viễn!")
                        }
                        currentList[idx2] = currentList[idx2].copy(
                            currentHp = newHp2,
                            isDead = isNpc2Dead,
                            memoryLogs = memory
                        )
                    }

                    // Create available skill for player to steal if combat occurred
                    val potentialLearn = if (Random.nextBoolean()) skill1 else skill2
                    val event = WorldEvent(
                        timestamp = timeString,
                        content = story,
                        relatedX = npc1.position.x,
                        relatedY = npc1.position.y,
                        sectorName = sector.name,
                        isCombatStory = true,
                        martialArtAvailable = potentialLearn
                    )

                    _events.value = _events.value + event
                    combatTriggered = true
                    break
                }
            }
        }

        _npcs.value = currentList
    }

    /**
     * Executes every 24 in-game simulation hours. Upgrades NPC knowledge, improves martial arts,
     * injects grey-zone behaviors, and updates historical chronicle logs.
     */
    private suspend fun runDailyUpdateCycle(day: Int) {
        logSystemEvent("--- Thiên Đạo Vô Hạn đại tuần hoàn: 24 giờ diễn sinh nhân quả giang hồ ---")
        val aliveNpcs = _npcs.value.filter { !it.isDead }
        if (aliveNpcs.isEmpty()) return

        // 1. Pick a legendary NPC and let them improve or modify a martial art using the power system AI
        val luckyCreator = aliveNpcs[Random.nextInt(aliveNpcs.size)]
        val envStory = "Bầu trời oán oán khí bốc lên ngùn ngụt, quần hùng rơi rụng cát bụi giang giang hoang vu."
        val modifiedArt = WuxiaAiApi.generateNewMartialArt(luckyCreator, envStory)
        
        val list = _npcs.value.toMutableList()
        val creatorIdx = list.indexOfFirst { it.id == luckyCreator.id }
        if (creatorIdx != -1) {
            val currentArts = list[creatorIdx].activeMartialArts.toMutableList()
            // replace or add
            if (currentArts.size >= 3) {
                currentArts.removeAt(0)
            }
            currentArts.add(modifiedArt)
            
            val memory = list[creatorIdx].memoryLogs.toMutableList()
            memory.add("Nghiên cứu kỳ thư thiên cơ cải biến võ công thành công thế hệ tiếp theo: [${modifiedArt.name}] chi phục thiên cổ!")
            
            // Gain dynamic base stats on success
            val currentBase = list[creatorIdx].basePower
            val updatedBase = currentBase * 1.35 // Upgrade basic kình lực

            list[creatorIdx] = list[creatorIdx].copy(
                activeMartialArts = currentArts,
                basePower = updatedBase,
                memoryLogs = memory
            )
            _npcs.value = list
            logSystemEvent("Vĩnh võ thần kỳ! ${luckyCreator.name} đã ngộ ra thiên phúc mới, cải tiến chiêu thức thành [${modifiedArt.name}]!")
        }

        // 2. Fetch Global General AI GM story update
        val playerStateText = "Tọa độ (${_player.value.position.x}, ${_player.value.position.y}), Võ công học được: ${_player.value.activeMartialArts.joinToString { it.name }}"
        val storySummary = WuxiaAiApi.generateWorldUpdate(
            dayCount = day,
            playerState = playerStateText,
            currentNpcs = aliveNpcs.take(12),
            recentEvents = _events.value.takeLast(6).map { it.content }
        )

        val dailyEvent = WorldEvent(
            timestamp = "Ngày $day, Giờ Tý (Diễn Hồn Bản)",
            content = storySummary,
            relatedX = luckyCreator.position.x,
            relatedY = luckyCreator.position.y,
            sectorName = getSectorAt(luckyCreator.position).name
        )

        _events.value = _events.value + dailyEvent
        logSystemEvent("Bản đồ võ lâm biến ảo rực rỡ! AI Vũ Giới cập nhật dữ liệu cốt truyện mới vào kênh giang hồ.")
    }

    /**
     * When player chooses to intervene in combat or target an NPC.
     * Locked IDs verify "overmasterkarinmered" is immortal. NPCs see them as beautiful youth.
     */
    suspend fun interveneCombat(targetNpcId: String): String {
        _isBusy.value = true
        val list = _npcs.value.toMutableList()
        val idx = list.indexOfFirst { it.id == targetNpcId }

        if (idx == -1 || list[idx].isDead) {
            _isBusy.value = false
            return "Kẻ địch rỗng không hoặc đã tử vong vĩnh viễn vùi thây hoang dã cát bụi."
        }

        val target = list[idx]
        val pl = _player.value
        val plSkill = pl.activeMartialArts[Random.nextInt(pl.activeMartialArts.size)]
        val targetSkill = target.activeMartialArts[Random.nextInt(target.activeMartialArts.size)]

        logSystemEvent("Thần ảnh của Vong Kình Thánh Tử [${pl.name}] hạ phàm can thiệp cuộc đời của ${target.name} bằng hào quang avatar tối thượng!")

        val narrative = WuxiaAiApi.generateCombatStoryline(pl, target, plSkill, targetSkill)

        // Player is immortal and infinitely strong in narrative, lets damage target HP
        val damage = Random.nextDouble(35.0, 75.0)
        val newTargetHp = (target.currentHp - damage).coerceAtLeast(0.0)
        val isTargetDead = newTargetHp <= 0.0

        val memory = target.memoryLogs.toMutableList()
        memory.add("Chạm trán avatar mộng cảnh hoang đường của đấng thần thông ${pl.name} (${pl.gender}) thất thoát nặng nề $damage% thọ nguyên.")
        if (isTargetDead) {
            memory.add("ĐỘT TỬ: Thần kình của tuyệt thế mĩ thuật dung mạo tiêu biến nguyên khí trần tục vĩnh viễn.")
            logSystemEvent("Vô cùng thảm khốc! Sự trừng phạt tối cao khiến ${target.name} hồn bay phách tán, chết thật vĩnh viễn!")
        }

        list[idx] = target.copy(
            currentHp = newTargetHp,
            isDead = isTargetDead,
            memoryLogs = memory
        )

        _npcs.value = list

        // Record on world channel
        val timeString = "Ngày ${_simulatedDay.value}, Giờ ${getFormattedHour(_simulatedHour.value)}"
        _events.value = _events.value + WorldEvent(
            timestamp = timeString,
            content = "PHÁN QUYẾT TỐI CAO: Thần thể ${pl.name} (${pl.gender}) thốt nhiên giáng thế, dập tắt ngọn nến sinh mệnh của ${target.name}. Tình oán oán báo liên quan triệt tiêu.",
            relatedX = target.position.x,
            relatedY = target.position.y,
            sectorName = getSectorAt(target.position).name
        )

        _isBusy.value = false
        saveGame()
        return narrative
    }

    /**
     * Player attempts to steal/learn a skill available on the world channel
     */
    fun learnSkillFromEvent(eventIndex: Int): Boolean {
        val evs = _events.value
        if (eventIndex !in evs.indices) return false
        val event = evs[eventIndex]
        val art = event.martialArtAvailable ?: return false

        // Check if player dynamic learning contains this skill
        val currentPlay = _player.value
        if (currentPlay.activeMartialArts.any { it.name == art.name }) return false

        val mArts = currentPlay.activeMartialArts.toMutableList()
        if (mArts.size >= 5) {
            mArts.removeAt(1) // Keep default talent at 0, replace old acquired ones
        }
        mArts.add(art)

        // Boost player base power from stolen knowledge
        val plPower = currentPlay.basePower * 1.5

        _player.value = currentPlay.copy(
            activeMartialArts = mArts,
            basePower = plPower
        )

        logSystemEvent("Vô Thượng Lĩnh Ngộ! Player thu nhận thành công võ kỳ văn [${art.name}] truyền xuất trực tiếp từ kênh giang hồ Vũ Giới!")
        saveGame()
        return true
    }

    private fun getCombatPowerRatioMultiplier(p1: CombatPower, p2: CombatPower): Double {
        return when {
            p1 > p2 -> 1.5
            p1 < p2 -> 0.6
            else -> 1.0
        }
    }

    private fun getFormattedHour(hour: Int): String {
        val terms = listOf(
            "Tý (0h-2h)", "Sửu (2h-4h)", "Dần (4h-6h)", "Mão (6h-8h)",
            "Thìn (8h-10h)", "Tị (10h-12h)", "Ngọ (12h-14h)", "Mùi (14h-16h)",
            "Thân (16h-18h)", "Dậu (18h-20h)", "Tuất (20h-22h)", "Hợi (22h-0h)"
        )
        return terms.getOrElse(hour / 2 % 12) { "$hour Giờ" }
    }

    private fun logSystemEvent(msg: String) {
        val timeString = "Hệ Thống - Ngày ${_simulatedDay.value}, Giờ ${getFormattedHour(_simulatedHour.value)}"
        _events.value = _events.value + WorldEvent(
            timestamp = timeString,
            content = msg,
            relatedX = _player.value.position.x,
            relatedY = _player.value.position.y,
            sectorName = "Thiên Đạo Trận Quy"
        )
    }

    // --- SAVE AND LOAD ---
    private fun saveGame() {
        try {
            val editor = prefs.edit()
            editor.putInt("simulated_day", _simulatedDay.value)
            editor.putInt("simulated_hour", _simulatedHour.value)
            
            // Basic custom serializers since Moshi compile plugins can sometimes be sensitive
            // We serialize to simple parseable lines or store in json representation
            val playerJson = serializeCharacter(_player.value)
            editor.putString("player_json", playerJson)

            val npcsJson = _npcs.value.map { serializeCharacter(it) }.joinToString("###_CHARACTER_SPLIT_###")
            editor.putString("npcs_json_split", npcsJson)

            val eventsJson = _events.value.map { serializeEvent(it) }.joinToString("###_EVENT_SPLIT_###")
            editor.putString("events_json_split", eventsJson)

            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Save Error: ${e.message}")
        }
    }

    private fun loadGame() {
        try {
            _simulatedDay.value = prefs.getInt("simulated_day", 1)
            _simulatedHour.value = prefs.getInt("simulated_hour", 0)

            val plStr = prefs.getString("player_json", null)
            if (plStr != null) {
                _player.value = deserializeCharacter(plStr)
            }

            val npcsStr = prefs.getString("npcs_json_split", null)
            if (!npcsStr.isNullOrBlank()) {
                _npcs.value = npcsStr.split("###_CHARACTER_SPLIT_###").map { deserializeCharacter(it) }
            }

            val evsStr = prefs.getString("events_json_split", null)
            if (!evsStr.isNullOrBlank()) {
                _events.value = evsStr.split("###_EVENT_SPLIT_###").map { deserializeEvent(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load Error: ${e.message}, loading defaults")
            resetWorld()
        }
    }

    // --- MANUAL HAND STRING SERIALIZATIONS FOR PERFECT STABILITY ---
    private fun serializeCharacter(c: GameCharacter): String {
        val artLines = c.activeMartialArts.joinToString("||") {
            "${it.name}|${it.description}|${it.abilityName}|${it.abilityEffect}|${it.basePowerMultiplier}|${it.modifiedCount}|${it.originCharId ?: "null"}"
        }
        val memoryLines = c.memoryLogs.joinToString("||") { it.replace("|", "_").replace(";", "_") }
        
        return "${c.id};${c.name};${c.isPlayer};${c.gender};${c.basePower.base};${c.basePower.exponent};${c.currentHp};$artLines;$memoryLines;${c.affiliation};${c.isDead};${c.behaviorTendency};${c.position.x};${c.position.y}"
    }

    private fun deserializeCharacter(s: String): GameCharacter {
        val parts = s.split(";")
        val id = parts[0]
        val name = parts[1]
        val isPlayer = parts[2].toBoolean()
        val gender = parts[3]
        val baseVal = parts[4].toDoubleOrNull() ?: 100.0
        val baseExp = parts[5].toIntOrNull() ?: 1
        val currentHp = parts[6].toDoubleOrNull() ?: 100.0

        val artsStr = parts.getOrNull(7) ?: ""
        val arts = if (artsStr.isBlank()) emptyList() else artsStr.split("||").map {
            val sub = it.split("|")
            MartialArt(
                name = sub[0],
                description = sub.getOrElse(1) { "" },
                abilityName = sub.getOrElse(2) { "" },
                abilityEffect = sub.getOrElse(3) { "" },
                basePowerMultiplier = sub.getOrNull(4)?.toDoubleOrNull() ?: 1.2,
                modifiedCount = sub.getOrNull(5)?.toIntOrNull() ?: 0,
                originCharId = if (sub.getOrNull(6) == "null") null else sub.getOrNull(6)
            )
        }

        val memStr = parts.getOrNull(8) ?: ""
        val memory = if (memStr.isBlank()) emptyList() else memStr.split("||")

        val aff = parts.getOrNull(9) ?: "Tự Do"
        val isDead = parts.getOrNull(10)?.toBoolean() ?: false
        val tend = parts.getOrNull(11) ?: "Neutral"
        val x = parts.getOrNull(12)?.toIntOrNull() ?: 50
        val y = parts.getOrNull(13)?.toIntOrNull() ?: 50

        return GameCharacter(
            id = id,
            name = name,
            isPlayer = isPlayer,
            gender = gender,
            basePower = CombatPower(baseVal, baseExp),
            currentHp = currentHp,
            activeMartialArts = arts,
            position = WorldPosition(x, y),
            memoryLogs = memory,
            affiliation = aff,
            isDead = isDead,
            behaviorTendency = tend
        )
    }

    private fun serializeEvent(v: WorldEvent): String {
        val artStr = if (v.martialArtAvailable != null) {
            val a = v.martialArtAvailable
            "${a.name}|${a.description}|${a.abilityName}|${a.abilityEffect}|${a.basePowerMultiplier}|${a.modifiedCount}|${a.originCharId ?: "null"}"
        } else "no_art"
        
        return "${v.timestamp};${v.content.replace(";", "_")};${v.relatedX};${v.relatedY};${v.sectorName};${v.isCombatStory};$artStr"
    }

    private fun deserializeEvent(s: String): WorldEvent {
        val parts = s.split(";")
        val timestamp = parts[0]
        val content = parts[1]
        val rx = parts[2].toIntOrNull() ?: 50
        val ry = parts[3].toIntOrNull() ?: 50
        val sectorName = parts[4]
        val isCombat = parts[5].toBoolean()
        
        val artStr = parts.getOrNull(6) ?: "no_art"
        val art = if (artStr == "no_art" || artStr.isBlank()) null else {
            val sub = artStr.split("|")
            MartialArt(
                name = sub[0],
                description = sub.getOrElse(1) { "" },
                abilityName = sub.getOrElse(2) { "" },
                abilityEffect = sub.getOrElse(3) { "" },
                basePowerMultiplier = sub.getOrNull(4)?.toDoubleOrNull() ?: 1.25,
                modifiedCount = sub.getOrNull(5)?.toIntOrNull() ?: 0,
                originCharId = if (sub.getOrNull(6) == "null") null else sub.getOrNull(6)
            )
        }

        return WorldEvent(
            timestamp = timestamp,
            content = content,
            relatedX = rx,
            relatedY = ry,
            sectorName = sectorName,
            isCombatStory = isCombat,
            martialArtAvailable = art
        )
    }
}
