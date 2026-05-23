package com.example.data

import kotlin.math.pow

// --- POWER SYSTEM ---
// Base-1024 Infinite Power Scaling
// Suffix: Ordinary -> KB -> MB -> GB -> TB -> PB -> EB -> ZB -> YB -> BB -> DB -> VC -> TH -> TD -> HN -> HV -> TT -> ETC...
data class CombatPower(
    val base: Double,
    val exponent: Int // index in POWER_UNITS
) : Comparable<CombatPower> {

    companion object {
        val POWER_UNITS = listOf(
            "Phàm",  // Ordinary
            "KB (Kình Bộ)",
            "MB (Minh Bản)",
            "GB (Gia Bản)",
            "TB (Thiên Bản)",
            "PB (Phong Bản)",
            "EB (Em Bản)",
            "ZB (Zenith Realm)",
            "YB (Yêu Bản)",
            "BB (Bá Bản)",
            "DB (Đại Bản)",
            "VC (Vô Cực)",
            "TH (Thăng Hoa)",
            "TD (Thiên Đạo)",
            "HN (Hỗn Nguyên)",
            "HV (Hư Vô)",
            "TT (Tối Thượng)"
        )

        fun fromDouble(value: Double): CombatPower {
            if (value <= 0.0) return CombatPower(0.0, 0)
            var b = value
            var e = 0
            while (b >= 1024.0 && e < POWER_UNITS.lastIndex) {
                b /= 1024.0
                e++
            }
            return CombatPower(b, e)
        }

        fun create(base: Double, exponent: Int): CombatPower {
            var b = base
            var e = exponent
            if (b <= 0.0) return CombatPower(0.0, 0)
            while (b >= 1024.0 && e < POWER_UNITS.lastIndex) {
                b /= 1024.0
                e++
            }
            while (b < 1.0 && e > 0) {
                b *= 1024.0
                e--
            }
            return CombatPower(b, e)
        }
    }

    fun toDisplayString(): String {
        val formattedBase = String.format("%.2f", base)
        val unit = if (exponent in POWER_UNITS.indices) POWER_UNITS[exponent] else "ETC (Thần cấp +${exponent - POWER_UNITS.lastIndex})"
        return "$formattedBase $unit"
    }

    override fun compareTo(other: CombatPower): Int {
        if (this.exponent != other.exponent) {
            return this.exponent.compareTo(other.exponent)
        }
        return this.base.compareTo(other.base)
    }

    operator fun plus(other: CombatPower): CombatPower {
        val targetExp = maxOf(this.exponent, other.exponent)
        val thisScaled = this.base / 1024.0.pow((targetExp - this.exponent).toDouble())
        val otherScaled = other.base / 1024.0.pow((targetExp - other.exponent).toDouble())
        return create(thisScaled + otherScaled, targetExp)
    }

    operator fun times(factor: Double): CombatPower {
        return create(this.base * factor, this.exponent)
    }

    operator fun minus(other: CombatPower): CombatPower {
        val targetExp = maxOf(this.exponent, other.exponent)
        val thisScaled = this.base / 1024.0.pow((targetExp - this.exponent).toDouble())
        val otherScaled = other.base / 1024.0.pow((targetExp - other.exponent).toDouble())
        val resultBase = thisScaled - otherScaled
        return if (resultBase <= 0.0) CombatPower(0.0, 0) else create(resultBase, targetExp)
    }
}

// --- MARTIAL ARTS (VÕ HỌC) ---
data class MartialArt(
    val name: String,
    val description: String,
    val abilityName: String, // Unique ability name
    val abilityEffect: String, // Specific rule description for custom combat modifier
    val basePowerMultiplier: Double = 1.2,
    val modifiedCount: Int = 0, // dynamic improvements by NPCs
    val originCharId: String? = null // who created/modified this
)

// --- POSITION IN THE 100x100 WORLD ---
data class WorldPosition(
    val x: Int,
    val y: Int
) {
    fun distanceTo(other: WorldPosition): Int {
        return kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    }
}

// --- WORLD SECTOR EVENTS (FACTIONS, REGIONS) ---
data class RegionSector(
    val xStart: Int,
    val yStart: Int,
    val name: String,
    val terrain: String, // e.g., "Thâm Sơn", "Sa Mạc", "Cổ Trấn", "Môn Phái", "Vùng Xám"
    val dangerLevel: String, // e.g., "Hỗn Loạn", "Trật Tự", "Nguy Hiểm Vùng Cực"
    val description: String
)

// --- CHARACTER (PLAYER OR NPC) ---
data class GameCharacter(
    val id: String, // locked as "overmasterkarinmered" for player, otherwise custom random string
    val name: String,
    val isPlayer: Boolean,
    val gender: String, // "Mỹ Thiếu Nữ" or "Mỹ Thiếu Nam" or custom
    val basePower: CombatPower,
    val currentHp: Double, // 100.0 is full, if 0 NPC dies, player cannot die
    val activeMartialArts: List<MartialArt>,
    val position: WorldPosition,
    val memoryLogs: List<String>, // memory of individual events, knowledge
    val affiliation: String = "Tự Do", // Faction: e.g. "Thiếu Lâm", "Cái Bang", "Hắc Đạo"
    val isDead: Boolean = false,
    val behaviorTendency: String = "Trung dung" // Neutral, Evil, Righteous, Gray
) {
    val currentCombatPower: CombatPower
        get() {
            var mult = 1.0
            activeMartialArts.forEach { mult *= it.basePowerMultiplier }
            return basePower * mult
        }
}

// --- CHRONICLE LOGS ON WORLD CHANNEL ---
data class WorldEvent(
    val timestamp: String, // Simulated game time, e.g. "Ngày 4, Giờ Tý"
    val content: String, // Rich-text story of gray-zone actions, cheat, robbery, betrayal
    val relatedX: Int,
    val relatedY: Int,
    val sectorName: String,
    val isCombatStory: Boolean = false,
    val martialArtAvailable: MartialArt? = null // Can be learned by Player if present and combat-story on channel
)
