package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle

class MainActivity : ComponentActivity() {
    private lateinit var sandbox: GameSandbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sandbox = GameSandbox(applicationContext)

        // Game auto-tick coroutine
        lifecycleScope.launch {
            while (true) {
                if (sandbox.isTicking.value && !sandbox.isBusy.value) {
                    sandbox.executeGameTick()
                }
                delay(7000) // tick every 7 seconds when active
            }
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WuxiaMahoganyBg),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    WuxiaMainScreen(
                        sandbox = sandbox,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WuxiaMainScreen(
    sandbox: GameSandbox,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core states
    val player by sandbox.player.collectAsStateWithLifecycle()
    val npcs by sandbox.npcs.collectAsStateWithLifecycle()
    val events by sandbox.events.collectAsStateWithLifecycle()
    val day by sandbox.simulatedDay.collectAsStateWithLifecycle()
    val hour by sandbox.simulatedHour.collectAsStateWithLifecycle()
    val isTicking by sandbox.isTicking.collectAsStateWithLifecycle()
    val isBusy by sandbox.isBusy.collectAsStateWithLifecycle()
    val sectors by sandbox.sectors.collectAsStateWithLifecycle()

    // UI Tab View within single screen
    var selectedTab by remember { mutableStateOf(0) } // 0: World Log, 1: Radar Grid Map, 2: NPC Profiles, 3: Mystic Player Profile
    
    // User configurations
    var isEditingName by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf(player.name) }
    var selectedGender by remember { mutableStateOf(player.gender) }
    var selectedNpcToObserve by remember { mutableStateOf<GameCharacter?>(null) }
    var selectedEventIndexForDialog by remember { mutableStateOf<Int?>(null) }
    
    // Search filter for NPCs
    var npcSearchQuery by remember { mutableStateOf("") }
    
    // Combat Narrative Dialog
    var activeCombatNarrative by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(player.name) {
        inputName = player.name
    }
    LaunchedEffect(player.gender) {
        selectedGender = player.gender
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WuxiaMahoganyBg)
            .padding(12.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WuxiaParchmentCard, shape = RoundedCornerShape(8.dp))
                .border(1.dp, WuxiaSoftBorder, shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VÔ HẠN VÕ LÂM AI",
                        color = WuxiaGleamingAmber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Live AI Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (WuxiaAiApi.isLiveAiEnabled) WuxiaVibrantEmerald.copy(alpha = 0.2f)
                                else WuxiaTextMuted.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (WuxiaAiApi.isLiveAiEnabled) WuxiaVibrantEmerald else WuxiaTextMuted,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (WuxiaAiApi.isLiveAiEnabled) "BẢN ĐỒ AI DÂN LẬP" else "MÔ PHỎNG NỘI BỘ",
                                color = if (WuxiaAiApi.isLiveAiEnabled) WuxiaVibrantEmerald else WuxiaTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Thời khắc: Ngày $day, Giờ ${getCanChiHour(hour)}",
                        color = WuxiaTextGolden,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Game Ticker Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Execute One Tick Manual
                IconButton(
                    onClick = {
                        scope.launch {
                            sandbox.executeGameTick()
                        }
                    },
                    enabled = !isBusy && !isTicking,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = WuxiaGleamingAmber,
                        disabledContentColor = WuxiaTextMuted
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = WuxiaGleamingAmber, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Tick manually")
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Toggle Auto-Tick Play/Pause
                Button(
                    onClick = {
                        sandbox.updateTickingState(!isTicking)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTicking) WuxiaCrimsonSlash else WuxiaVibrantEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Icon(
                        imageVector = if (isTicking) Icons.Filled.Close else Icons.Filled.PlayArrow,
                        contentDescription = "Auto tick toggle",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTicking) "NGỪNG" else "TỰ ĐỘNG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- DYNAMIC TABS BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WuxiaParchmentCard, shape = RoundedCornerShape(6.dp))
                .border(1.dp, WuxiaSoftBorder, shape = RoundedCornerShape(6.dp))
                .padding(2.dp)
        ) {
            val tabs = listOf("Vũ Giới LOG", "Kỳ Thư BẢN ĐỒ", "NHÂN VẬT KỶ", "TỰ PHẾ THẦN")
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selectedTab == index) WuxiaGleamingAmber.copy(alpha = 0.15f) else Color.Transparent
                        )
                        .border(
                            width = if (selectedTab == index) 1.dp else 0.dp,
                            color = if (selectedTab == index) WuxiaGleamingAmber else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (selectedTab == index) WuxiaGleamingAmber else WuxiaTextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- MAIN TABS CONTENT AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(WuxiaParchmentCard, shape = RoundedCornerShape(8.dp))
                .border(1.dp, WuxiaSoftBorder, shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            when (selectedTab) {
                0 -> { // World Log Channel
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kênh Vũ Giới Phong Vân",
                                color = WuxiaGleamingAmber,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { sandbox.resetWorld() }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Reset world", tint = WuxiaCrimsonSlash, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        Divider(color = WuxiaSoftBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        if (events.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Đang diễn sinh thiên địa kình khí khởi tạo giang hồ...",
                                    color = WuxiaTextMuted,
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        } else {
                            val listState = rememberLazyListState()
                            
                            // Auto scroll to bottom when new logs income
                            LaunchedEffect(events.size) {
                                if (events.isNotEmpty()) {
                                    listState.animateScrollToItem(events.lastIndex)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(events) { index, ev ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (ev.isCombatStory) WuxiaCrimsonSlash.copy(alpha = 0.05f)
                                                else Color.White.copy(alpha = 0.012f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (ev.isCombatStory) WuxiaCrimsonSlash.copy(alpha = 0.2f) else WuxiaSoftBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedEventIndexForDialog = index }
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = ev.timestamp,
                                                    color = if (ev.isCombatStory) WuxiaCrimsonSlash else WuxiaTextGolden,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "[${ev.sectorName}]",
                                                    color = WuxiaTextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = ev.content,
                                                color = WuxiaTextGolden,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            
                                            // Stolen martial art option indicator
                                            if (ev.martialArtAvailable != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val exists = player.activeMartialArts.any { it.name == ev.martialArtAvailable.name }
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(
                                                            if (exists) WuxiaTextMuted.copy(alpha = 0.2f) else WuxiaGleamingAmber.copy(alpha = 0.15f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .clickable(enabled = !exists) {
                                                            val success = sandbox.learnSkillFromEvent(index)
                                                            if (success) {
                                                                Toast.makeText(context, "Lĩnh ngộ thành công: ${ev.martialArtAvailable.name}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (exists) Icons.Filled.Check else Icons.Filled.Add,
                                                        contentDescription = "Steal skill",
                                                        tint = if (exists) WuxiaTextMuted else WuxiaGleamingAmber,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (exists) "Đã Lĩnh Ngộ [${ev.martialArtAvailable.name}]"
                                                               else "Lĩnh Ngộ Võ Học Phong Vân [${ev.martialArtAvailable.name}]",
                                                        color = if (exists) WuxiaTextMuted else WuxiaGleamingAmber,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // Radar Grid Map 100x100
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Kỳ Thư Bản Đồ 100x100 (Thống Trị Không Gian)",
                            color = WuxiaGleamingAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tọa độ Thần Thể Player: (${player.position.x}, ${player.position.y}) | NPCs Gần Kề: ${npcs.filter { !it.isDead }.size} cao thủ",
                            color = WuxiaTextMuted,
                            fontSize = 11.sp
                        )
                        Divider(color = WuxiaSoftBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Render Radar Map Canvas representation of the virtual grid
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, WuxiaSoftBorder, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            WuxiaVirtualRadarGrid(
                                player = player,
                                npcs = npcs,
                                sectors = sectors,
                                onCoordinateSelected = { selectedX, selectedY ->
                                    sandbox.teleportPlayer(WorldPosition(selectedX, selectedY))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gõ/Chạm bất cứ vùng tọa độ nào trên lưới để di tản Thần Thể. Hệ thống lock player không tương tác trực tiếp với NPC cấp thấp trừ khi xuất hiện Combat Avatar.",
                            color = WuxiaTextMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                2 -> { // NPC Directory History Tracker
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Kỷ Lục Quần Hùng (NPC Ký Ức & Cải Biến)",
                            color = WuxiaGleamingAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        // Search field
                        OutlinedTextField(
                            value = npcSearchQuery,
                            onValueChange = { npcSearchQuery = it },
                            placeholder = { Text("Tìm tên cao thủ, bang phái...", color = WuxiaTextMuted, fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            textStyle = TextStyle(color = WuxiaTextGolden, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WuxiaGleamingAmber,
                                unfocusedBorderColor = WuxiaSoftBorder,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true
                        )

                        Divider(color = WuxiaSoftBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))

                        val filteredNpcs = npcs.filter {
                            it.name.contains(npcSearchQuery, ignoreCase = true) ||
                            it.affiliation.contains(npcSearchQuery, ignoreCase = true)
                        }

                        if (filteredNpcs.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text("Không tìm thấy tàn tích của hào kiệt nào.", color = WuxiaTextMuted, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(filteredNpcs) { _, npc ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (npc.isDead) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.01f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (npc.isDead) WuxiaSoftBorder else WuxiaSoftBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedNpcToObserve = npc }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = npc.name,
                                                        color = if (npc.isDead) WuxiaCrimsonSlash else WuxiaTextGolden,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontStyle = if (npc.isDead) FontStyle.Italic else FontStyle.Normal
                                                    )
                                                    if (npc.isDead) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(3.dp))
                                                                .background(WuxiaCrimsonSlash.copy(alpha = 0.2f))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("PHẾ TRẦN (CHẾT VĨNH VIỄN)", color = WuxiaCrimsonSlash, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Bang phái: ${npc.affiliation} | Xu hướng: ${npc.behaviorTendency}",
                                                    color = WuxiaTextMuted,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "Chiến lực kịch bản: ${npc.currentCombatPower.toDisplayString()}",
                                                    color = WuxiaGleamingAmber,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            // Action to intervene if alive
                                            if (!npc.isDead) {
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            val result = sandbox.interveneCombat(npc.id)
                                                            activeCombatNarrative = result
                                                            Toast.makeText(context, "Sát phạt kịch tính giáng tông!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = WuxiaCrimsonSlash.copy(alpha = 0.2f),
                                                        contentColor = WuxiaCrimsonSlash
                                                    ),
                                                    shape = RoundedCornerShape(4.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                                ) {
                                                    Text("CAN THIỆP", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> { // Player Stats & Setup
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Tự Thuật Chí Cao Vong Kình Thần",
                            color = WuxiaGleamingAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hồ sơ đấng tối tại Vế Thượng. Chỉ ID \"overmasterkarinmered\" tồn tại bất tử vĩnh hằng.",
                            color = WuxiaTextMuted,
                            fontSize = 11.sp
                        )
                        Divider(color = WuxiaSoftBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Name Editor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isEditingName) {
                                TextField(
                                    value = inputName,
                                    onValueChange = { inputName = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(color = WuxiaTextGolden, fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WuxiaGleamingAmber,
                                        unfocusedBorderColor = WuxiaSoftBorder
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        sandbox.setPlayerName(inputName)
                                        isEditingName = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WuxiaVibrantEmerald)
                                ) {
                                    Text("LƯU", fontSize = 11.sp)
                                }
                            } else {
                                Text(
                                    text = "Thần danh: ${player.name}",
                                    color = WuxiaTextGolden,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { isEditingName = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = WuxiaSoftBorder)
                                ) {
                                    Text("SỬA", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Perception selection: Mỹ thiếu nam/Mỹ thiếu nữ
                        Text(
                            text = "Dung mạo ảo ảnh trong mắt NPC:",
                            color = WuxiaTextMuted,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Mỹ Thiếu Nữ", "Mỹ Thiếu Nam").forEach { item ->
                                val active = player.gender == item
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) WuxiaGleamingAmber.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.dp, if (active) WuxiaGleamingAmber else WuxiaSoftBorder, RoundedCornerShape(4.dp))
                                        .clickable { sandbox.setPlayerGender(item) }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item, color = if (active) WuxiaGleamingAmber else WuxiaTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Power specifications
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                .border(1.dp, WuxiaSoftBorder, shape = RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Khởi Thiên Thần Lực (Chiến lực Vô Hạn):", color = WuxiaTextMuted, fontSize = 11.sp)
                                Text(
                                    player.currentCombatPower.toDisplayString(),
                                    color = WuxiaGleamingAmber,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Kha năng: Vô Hạn thăng quan. Thần kình tự động nhân tỉ lệ bội luỹ theo võ học lĩnh ngũ trên Vũ Giới Phong Vân.",
                                    color = WuxiaTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Arts list
                        Text(
                            text = "Tiên Võ Công Tích Hợp (${player.activeMartialArts.size}/5 chiêu thức):",
                            color = WuxiaTextGolden,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        player.activeMartialArts.forEach { art ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(6.dp))
                                    .border(1.dp, WuxiaSoftBorder, shape = RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                        Text(art.name, color = WuxiaGleamingAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Hệ số: x${art.basePowerMultiplier}", color = WuxiaVibrantEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(art.description, color = WuxiaTextGolden, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .background(WuxiaOceanCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Ability: <${art.abilityName}> | ${art.abilityEffect}", color = WuxiaOceanCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        
        // --- BOTTOM PROGRESS LOADER INDICATOR ---
        if (isBusy) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = WuxiaGleamingAmber,
                trackColor = Color.Transparent
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }

    // --- DETAILED CUSTOM EVENT MODAL DIALOG ---
    if (selectedEventIndexForDialog != null) {
        val ev = events[selectedEventIndexForDialog!!]
        AlertDialog(
            onDismissRequest = { selectedEventIndexForDialog = null },
            title = {
                Text(
                    text = "${ev.timestamp} | ${ev.sectorName}",
                    color = WuxiaGleamingAmber,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
                Column {
                    Text(
                        text = ev.content,
                        color = WuxiaTextGolden,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    if (ev.martialArtAvailable != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WuxiaGleamingAmber.copy(alpha = 0.05f), shape = RoundedCornerShape(6.dp))
                                .border(1.dp, WuxiaGleamingAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Bí văn võ công tàn thư xuất hiện:", color = WuxiaGleamingAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(ev.martialArtAvailable.name, color = WuxiaTextGolden, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(ev.martialArtAvailable.description, color = WuxiaTextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Khả năng: <${ev.martialArtAvailable.abilityName}> - ${ev.martialArtAvailable.abilityEffect}", color = WuxiaOceanCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEventIndexForDialog = null }) {
                    Text("ĐẤU THẢO", color = WuxiaGleamingAmber)
                }
            },
            containerColor = WuxiaParchmentCard,
            shape = RoundedCornerShape(10.dp)
        )
    }

    // --- NPC OBSERVANCE MEMORY MODAL ---
    if (selectedNpcToObserve != null) {
        val npc = selectedNpcToObserve!!
        AlertDialog(
            onDismissRequest = { selectedNpcToObserve = null },
            title = {
                Text(
                    text = "Bản đồ sinh mạng: ${npc.name}",
                    color = WuxiaGleamingAmber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Trạng thái: " + if (npc.isDead) "Đã tử trận vong phế" else "Còn sống (${npc.currentHp.toInt()}% Sinh Lực)", color = WuxiaTextGolden, fontSize = 12.sp)
                    Text("Chiến lực: ${npc.currentCombatPower.toDisplayString()}", color = WuxiaGleamingAmber, fontSize = 12.sp)
                    Text("Xu hướng tính cách: ${npc.behaviorTendency}", color = WuxiaTextGolden, fontSize = 12.sp)
                    Text("Nơi chốn hiện tại: Tọa độ (${npc.position.x}, ${npc.position.y})", color = WuxiaTextMuted, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Thiên Kình Võ Công Sở Hữu:", color = WuxiaGleamingAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    npc.activeMartialArts.forEach { art ->
                        Text("- ${art.name}: ${art.description}", color = WuxiaTextGolden, fontSize = 11.sp)
                        Text("  Chiêu: <${art.abilityName}> | ${art.abilityEffect}", color = WuxiaOceanCyan, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ghi Chép Lịch Sử Kỳ Thư / Chí Nhớ:", color = WuxiaGleamingAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    npc.memoryLogs.forEach { log ->
                        Text("• $log", color = WuxiaTextGolden, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNpcToObserve = null }) {
                    Text("RÚT LUI", color = WuxiaGleamingAmber)
                }
            },
            containerColor = WuxiaParchmentCard,
            shape = RoundedCornerShape(10.dp)
        )
    }

    // --- ACTION RESULT COMBAT INTERVENTION DIALOG ---
    if (activeCombatNarrative != null) {
        AlertDialog(
            onDismissRequest = { activeCombatNarrative = null },
            title = {
                Text(
                    text = "Vũ Giới Phong Vân: Can Thiệp Chân Chân Lục",
                    color = WuxiaGleamingAmber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = activeCombatNarrative!!,
                        color = WuxiaTextGolden,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeCombatNarrative = null }) {
                    Text("ĐỒNG Ý", color = WuxiaGleamingAmber)
                }
            },
            containerColor = WuxiaParchmentCard,
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
fun WuxiaVirtualRadarGrid(
    player: GameCharacter,
    npcs: List<GameCharacter>,
    sectors: List<RegionSector>,
    onCoordinateSelected: (Int, Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // Canvas doing radar coordinates mapping
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    // Let's figure out coordinates on 100x100
                    // For safety in calculations, we intercept gestures
                }
        ) {
            val gridStepX = widthPx / 100f
            val gridStepY = heightPx / 100f

            // Draw sectors visual background regions
            sectors.forEach { sec ->
                val sX = sec.xStart * gridStepX
                val sY = sec.yStart * gridStepY
                val sW = 30f * gridStepX
                val sH = 30f * gridStepY

                drawRect(
                    color = when (sec.name) {
                        "Băng Cực Cốc" -> Color(0xFF1E3A5F).copy(alpha = 0.2f)
                        "Thiếu Lâm Tự" -> Color(0xFF4A3B12).copy(alpha = 0.2f)
                        "Tiêu Dao Am" -> Color(0xFF1B4D3E).copy(alpha = 0.2f)
                        "Dã Ngoại Vùng Xám" -> Color(0xFF3B2F2F).copy(alpha = 0.2f)
                        "Ác Nhân Cốc" -> Color(0xFF4A1A12).copy(alpha = 0.2f)
                        else -> Color(0xFF3D2D1B).copy(alpha = 0.2f)
                    },
                    topLeft = Offset(sX, sY),
                    size = androidx.compose.ui.geometry.Size(sW, sH)
                )
            }

            // Draw clean coordinate grid divisions (every 10 units)
            for (i in 0..10) {
                drawLine(
                    color = WuxiaSoftBorder.copy(alpha = 0.3f),
                    start = Offset(i * 10 * gridStepX, 0f),
                    end = Offset(i * 10 * gridStepX, heightPx),
                    strokeWidth = 1f
                )
                drawLine(
                    color = WuxiaSoftBorder.copy(alpha = 0.3f),
                    start = Offset(0f, i * 10 * gridStepY),
                    end = Offset(widthPx, i * 10 * gridStepY),
                    strokeWidth = 1f
                )
            }

            // Draw alive NPCs positions
            npcs.filter { !it.isDead }.forEach { npc ->
                val nX = npc.position.x * gridStepX
                val nY = npc.position.y * gridStepY

                drawCircle(
                    color = when (npc.behaviorTendency) {
                        "Evil" -> WuxiaCrimsonSlash
                        "Righteous" -> WuxiaVibrantEmerald
                        "Gray" -> WuxiaTextMuted
                        else -> WuxiaTextGolden
                    },
                    radius = 4.dp.toPx(),
                    center = Offset(nX, nY)
                )
            }

            // Draw player position glowing aura
            val pX = player.position.x * gridStepX
            val pY = player.position.y * gridStepY
            drawCircle(
                color = WuxiaGleamingAmber,
                radius = 6.dp.toPx(),
                center = Offset(pX, pY)
            )
            drawCircle(
                color = WuxiaGleamingAmber.copy(alpha = 0.3f),
                radius = 12.dp.toPx(),
                center = Offset(pX, pY),
                style = Stroke(width = 2f)
            )
        }

        // Tap gestures proxy using Box layer overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInputGestureDetector { tapOffset ->
                    val xUnit = ((tapOffset.x / widthPx) * 100).toInt().coerceIn(1, 99)
                    val yUnit = ((tapOffset.y / heightPx) * 100).toInt().coerceIn(1, 99)
                    onCoordinateSelected(xUnit, yUnit)
                }
        )
    }
}

// Helper Compose gesture extension to prevent any build block setups
@Suppress("ModifierFactoryUnreferencedReceiver")
fun Modifier.pointerInputGestureDetector(onTap: (Offset) -> Unit): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onTap = { offset ->
            onTap(offset)
        }
    )
}

fun getCanChiHour(hour: Int): String {
    val canChi = listOf(
        "Tý (Đêm)", "Sửu (Đêm muộn)", "Dần (Rạng Đông)", "Mão (Bình Minh)",
        "Thìn (Sáng)", "Tị (Trưa muộn)", "Ngọ (Chính Trưa)", "Mùi (Chiều sương)",
        "Thân (Chiều muộn)", "Dậu (Hoàng Hôn)", "Tuất (Chạng Vạng)", "Hợi (Nửa Đêm)"
    )
    return canChi.getOrElse(hour / 2 % 12) { "$hour giờ" }
}
