package com.marsguide.vzd.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.data.remote.AuthenticMarsDatasets
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.util.SoundManager
import java.util.Locale

data class KnowledgeItem(
    val id: String,
    val category: KnowledgeCategory,
    val titleEn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val detailsEn: String,
    val detailsAr: String,
    val telemetrySpecs: Map<String, String>,
    val sourceDataset: String
)

enum class KnowledgeCategory(val labelEn: String, val labelAr: String) {
    ALL("ALL", "الكل"),
    LANDMARKS("LANDMARKS", "المعالم"),
    ROVERS("ROVERS & LANDERS", "المركبات"),
    ATMOSPHERE("ATMOSPHERE", "الغلاف الجوي"),
    ASTROBIOLOGY("ASTROBIOLOGY", "علم الأحياء الفلكي")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarsKnowledgeTerminalScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(KnowledgeCategory.ALL) }
    var isArabic by remember { mutableStateOf(false) }

    val knowledgeBase = remember {
        listOf(
            KnowledgeItem(
                id = "olympus_mons",
                category = KnowledgeCategory.LANDMARKS,
                titleEn = "Olympus Mons",
                titleAr = "أوليمبوس مونز",
                subtitleEn = "Largest Volcano in the Solar System",
                subtitleAr = "أكبر بركان في النظام الشمسي",
                detailsEn = "A massive shield volcano standing approximately 22 kilometers (14 miles) high—nearly three times the height of Mount Everest. Its base spans over 600 kilometers, created by repeated effusive lava flows over billions of years.",
                detailsAr = "بركان درعي ضخم يبلغ ارتفاعه حوالي 22 كيلومتراً (ثلاثة أضعاف جبار جبل إيفرست). تمتد قاعدته لأكثر من 600 كيلومتر نتيجة لتدفقات الحمم البركانية المتكررة عبر مليارات السنين.",
                telemetrySpecs = mapOf("Elevation" to "+21,229 m", "Base Diameter" to "600 km", "Caldera Depth" to "3.2 km", "Age" to "Hesperian to Amazonian"),
                sourceDataset = AuthenticMarsDatasets.PDS_MOLA_SOURCE.name
            ),
            KnowledgeItem(
                id = "valles_marineris",
                category = KnowledgeCategory.LANDMARKS,
                titleEn = "Valles Marineris",
                titleAr = "فاليس مارينريس",
                subtitleEn = "Grand Canyon of Mars",
                subtitleAr = "أخدود مارينريس العظيم",
                detailsEn = "A vast system of canyons stretching over 4,000 kilometers long, up to 200 kilometers wide, and 7 kilometers deep. Unlike Earth's Grand Canyon formed by rivers, Valles Marineris is a colossal tectonic graben fracture.",
                detailsAr = "نظام هائل من الأخدود يمتد لأكثر من 4000 كيلومتر، بعرض يصل إلى 200 كيلومتر وعمق 7 كيلومترات. على عكس أخدود الأرض، تشكل نتيجة تصدع تكتوني ضخم.",
                telemetrySpecs = mapOf("Length" to "4,000 km", "Max Depth" to "7.0 km", "Width" to "200 km", "Origin" to "Tectonic Rifting"),
                sourceDataset = AuthenticMarsDatasets.USGS_GAZETTEER_SOURCE.name
            ),
            KnowledgeItem(
                id = "jezero_crater",
                category = KnowledgeCategory.LANDMARKS,
                titleEn = "Jezero Crater",
                titleAr = "فوهة جيزيرو",
                subtitleEn = "Ancient Lake Bed & Rover Landing Site",
                subtitleAr = "قاع بحيرة قديمة وموقع هبوط روفر بيرسيفيرانس",
                detailsEn = "A 49-kilometer-wide impact crater featuring a well-preserved ancient river delta. Selected as the landing site for NASA's Perseverance rover to search for biosignatures and cache rock samples.",
                detailsAr = "فوهة اصطدامية يبلغ عرضها 49 كم تضم دلتا نهرية قديمة محفوظة جيداً. اختيرت كموقع هبوط لروفر بيرسيفيرانس التابع لناسا للبحث عن المؤشرات الحيوية.",
                telemetrySpecs = mapOf("Diameter" to "49.0 km", "Latitude" to "18.38° N", "Longitude" to "77.58° E", "Target" to "Astrobiology / Clay Deltas"),
                sourceDataset = AuthenticMarsDatasets.PDS_HIRISE_SOURCE.name
            ),
            KnowledgeItem(
                id = "perseverance_rover",
                category = KnowledgeCategory.ROVERS,
                titleEn = "Perseverance Rover (Mars 2020)",
                titleAr = "المثابرة (مارس 2020)",
                subtitleEn = "Astrobiology & Sample Caching Laboratory",
                subtitleAr = "مختبر البحث عن الحياة وتخزين العينات",
                detailsEn = "Equipped with SHERLOC, PIXL, SuperCam, and MOXIE (oxygen generator). Perseverance is actively drilling core samples, sealing them in titanium tubes, and characterizing Martian astrobiology.",
                detailsAr,
                detailsAr = "مجهز بأجهزة SHERLOC و PIXL و SuperCam و MOXIE (مولد الأكسجين). يقوم بيرسيفيرانس بحفر عينات أساسية وختمها في أنابيب تيتانيوم.",
                telemetrySpecs = mapOf("Launch" to "July 30, 2020", "Landing" to "Feb 18, 2021", "Power" to "MMRTG Nuclear", "Status" to "Operational"),
                sourceDataset = "NASA Mars 2020 Mission Archive"
            ),
            KnowledgeItem(
                id = "curiosity_rover",
                category = KnowledgeCategory.ROVERS,
                titleEn = "Curiosity Rover (MSL)",
                titleAr = "كيوريوسيتي (مختبر علوم المريخ)",
                subtitleEn = "Mount Sharp Gale Crater Explorer",
                subtitleAr = "مستكشف جبل شارب في فوهة غيل",
                detailsEn = "Exploring Gale Crater since 2012. Curiosity has confirmed that ancient Mars possessed chemical and environmental conditions favorable for microbial life, analyzing clay and sulfate strata.",
                detailsAr = "يستكشف فوهة غيل منذ عام 2012. أكد كيوريوسيتي أن المريخ القديم امتلك ظروفاً بيئية وكيميائية ملائمة للحياة الميكروبية من خلال تحليل طبقات الطين.",
                telemetrySpecs = mapOf("Launch" to "Nov 26, 2011", "Landing" to "Aug 6, 2012", "Traversed" to "30+ km", "Status" to "Operational"),
                sourceDataset = "NASA MSL / PDS Archive"
            ),
            KnowledgeItem(
                id = "mars_atmosphere",
                category = KnowledgeCategory.ATMOSPHERE,
                titleEn = "Atmospheric Composition & Weather",
                titleAr = "تركيبة الغلاف الجوي والطقس",
                subtitleEn = "CO2 Dominance & Dust Dynamics",
                subtitleAr = "هيمنة ثاني أكسيد الكربون وديناميكيات الغبار",
                detailsEn = "Mars has a thin atmosphere composed of 95.3% Carbon Dioxide, 2.6% Nitrogen, 1.9% Argon, and trace oxygen/water vapor. Surface pressure averages 610 Pascals (less than 1% of Earth's).",
                detailsAr = "يملك المريخ غلافاً جوياً رقيقاً يتكون من 95.3% ثاني أكسيد الكربون، 2.6% نيتروجين، 1.9% أرغون، وآثار من الأكسجين وبخار الماء. يبلغ الضغط السطحي 610 باسكال.",
                telemetrySpecs = mapOf("CO2" to "95.3%", "Nitrogen" to "2.6%", "Surface Pressure" to "610 Pa", "Mean Temp" to "-60°C"),
                sourceDataset = "NASA REMS / MEDA Atmospheric Telemetry"
            ),
            KnowledgeItem(
                id = "astrobiology_manual",
                category = KnowledgeCategory.ASTROBIOLOGY,
                titleEn = "Astrobiology & Biosignatures",
                titleAr = "علم الأحياء الفلكي والمؤشرات الحيوية",
                subtitleEn = "Search for Ancient Extraterrestrial Life",
                subtitleAr = "البحث عن حياة خارج الأرض",
                detailsEn = "Focuses on detecting organic carbon molecules, stromatolites, phosphating minerals, and subsurface liquid aquifers. Clay-rich lacustrine deposits in Jezero and Gale are prime preservation targets.",
                detailsAr = "يركز على اكتشاف جزيئات الكربون العضوية، الستروماتوليت، والمعادن الفوسفاتية، والمياه الجوفية. الرواسب الغنية بالطين في جيزيرو وغيل هي الأهداف الرئيسية.",
                telemetrySpecs = mapOf("Key Target" to "Lacustrine Clays", "Marker" to "Aromatic Carbon", "Habitability" to "Subsurface Aquifers"),
                sourceDataset = "NASA Astrobiology Roadmap PDS"
            )
        )
    }

    val filteredList = knowledgeBase.filter { item ->
        val matchesCategory = selectedCategory == KnowledgeCategory.ALL || item.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
                item.titleEn.contains(searchQuery, ignoreCase = true) ||
                item.titleAr.contains(searchQuery, ignoreCase = true) ||
                item.subtitleEn.contains(searchQuery, ignoreCase = true) ||
                item.detailsEn.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .testTag("mars_knowledge_terminal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // --- TOP TERMINAL HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VZD // SCIENTIFIC KNOWLEDGE TERMINAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = if (isArabic) "موسوعة المريخ العلمية" else "AUTHENTIC MARS ENCYCLOPEDIA",
                        style = MaterialTheme.typography.titleLarge,
                        color = MarsOrangeBright,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Language Toggle (EN / AR)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, ScienceTeal.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .clickable {
                            SoundManager.playClick()
                            isArabic = !isArabic
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = SpaceCardDark
                ) {
                    Text(
                        text = if (isArabic) "EN" else "عربي",
                        color = ScienceTeal,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (isArabic) "بحث في المعالم، المركبات، الغلاف الجوي..." else "Search landmarks, rovers, atmosphere, astrobiology...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ScienceTeal)
                },
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScienceTeal,
                    unfocusedBorderColor = SpaceBorderDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ScienceTeal,
                    focusedContainerColor = SpaceCardDark,
                    unfocusedContainerColor = SpaceCardDark
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- CATEGORY TABS ---
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(KnowledgeCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (isSelected) MarsOrangeBright else SpaceBorderDark,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                SoundManager.playClick()
                                selectedCategory = category
                            },
                        color = if (isSelected) MarsOrangeBright.copy(alpha = 0.2f) else SpaceCardDark
                    ) {
                        Text(
                            text = if (isArabic) category.labelAr else category.labelEn,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MarsOrangeBright else Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- KNOWLEDGE ITEMS LIST ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { item ->
                    KnowledgeCard(item = item, isArabic = isArabic)
                }

                if (filteredList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "لا توجد نتائج مطابقة لبحثك" else "NO KNOWLEDGE MATCHES FOUND",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeCard(item: KnowledgeItem, isArabic: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, SpaceBorderDark, RoundedCornerShape(4.dp)),
        color = SpaceCardDark.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MarsOrangeBright,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SRC: ${item.sourceDataset}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isArabic) item.titleAr else item.titleEn,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isArabic) item.subtitleAr else item.subtitleEn,
                style = MaterialTheme.typography.bodySmall,
                color = ScienceTeal,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isArabic) item.detailsAr else item.detailsEn,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry specs pills
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.telemetrySpecs.forEach { (key, value) ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .border(1.dp, ScienceTeal.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                        color = SpaceBlack
                    ) {
                        Text(
                            text = "$key: $value",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ScienceTeal,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
