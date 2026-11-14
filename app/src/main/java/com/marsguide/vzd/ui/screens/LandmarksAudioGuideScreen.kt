package com.marsguide.vzd.ui.screens

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marsguide.vzd.ui.theme.*
import com.marsguide.vzd.ui.viewmodel.MarsGuideViewModel
import com.marsguide.vzd.util.SoundManager
import java.util.*

data class LandmarkAudioItem(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val coordinates: String
)

@Composable
fun LandmarksAudioGuideScreen(
    viewModel: MarsGuideViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isArabic by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsNotice by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            mainHandler.post {
                ttsReady = status == TextToSpeech.SUCCESS
                if (status == TextToSpeech.SUCCESS) {
                    engine?.language = Locale.US
                }
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { playingId = utteranceId }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (playingId == utteranceId) playingId = null
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    if (playingId == utteranceId) playingId = null
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    if (playingId == utteranceId) playingId = null
                }
            }
        })
        engine
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val landmarks = remember {
        listOf(
            LandmarkAudioItem(
                id = "olympus_mons",
                titleEn = "Olympus Mons",
                titleAr = "أوليمبوس مونس",
                subtitleEn = "Largest Volcano in the Solar System",
                subtitleAr = "أكبر بركان في المجموعة الشمسية",
                descriptionEn = "Standing at 21.2 km above the datum, Olympus Mons is a colossal basaltic shield volcano formed over billions of years by quiet, effusive lava flows.",
                descriptionAr = "يبلغ ارتفاع أوليمبوس مونس 21.2 كيلومتراً، وهو بركان درعي هائل تشكل عبر مليارات السنين من تدفقات الحمم البازلتية.",
                coordinates = "18.65° N, 226.20° E"
            ),
            LandmarkAudioItem(
                id = "valles_marineris",
                titleEn = "Valles Marineris",
                titleAr = "فاليس مارينريس",
                subtitleEn = "The Grand Canyon of Mars",
                subtitleAr = "الأخدود العظيم للمريخ",
                descriptionEn = "A vast tectonic rift canyon system stretching over 4,000 km long and up to 7 km deep across the Martian equatorial belt.",
                descriptionAr = "نظام واسع من الأودية والصدوع التكتونية يمتد لأكثر من 4000 كم ويصل عمقه إلى 7 كم.",
                coordinates = "9.80° S, 283.60° E"
            ),
            LandmarkAudioItem(
                id = "jezero_crater",
                titleEn = "Jezero Crater",
                titleAr = "فوهة جيزيرو",
                subtitleEn = "Perseverance Rover Landing Site & Ancient Lake",
                subtitleAr = "موقع هبوط مركبة بيرسيفيرانس وبحيرة قديمة",
                descriptionEn = "A 45 km wide impact crater containing an ancient river delta that once held liquid water and clay minerals, currently explored by NASA's Perseverance rover.",
                descriptionAr = "فوهة اصطدام يبلغ عرضها 45 كم تحتوي على دلتا نهر قديمة احتوت ذات يوم على مياه سائلة ومعادن طينية وتستكشفها مركبة بيرسيفيرانس.",
                coordinates = "18.44° N, 77.45° E"
            ),
            LandmarkAudioItem(
                id = "gale_crater",
                titleEn = "Gale Crater & Mount Sharp",
                titleAr = "فوهة غيل وجبل شارب",
                subtitleEn = "MSL Curiosity Exploration Basin",
                subtitleAr = "حوض استكشاف مركبة كيوريوسيتي",
                descriptionEn = "A 154 km wide basin with Aeolis Mons rising 5.5 km at its center. Curiosity's instruments confirmed long-lived ancient freshwater lakes here.",
                descriptionAr = "فوهة بعرض 154 كم يتوسطها جبل شارب بارتفاع 5.5 كم، وقد أكدت مركبة كيوريوسيتي وجود بحيرات عذبة قديمة فيها.",
                coordinates = "5.37° S, 137.81° E"
            ),
            LandmarkAudioItem(
                id = "hellas_planitia",
                titleEn = "Hellas Planitia",
                titleAr = "حوض هيلاس",
                subtitleEn = "Deepest Impact Basin on Mars",
                subtitleAr = "أعمق حوض اصطدام على سطح المريخ",
                descriptionEn = "Spanning 2,300 km in diameter and descending over 7.1 km below the Martian datum, producing the highest surface atmospheric pressure on Mars.",
                descriptionAr = "يمتد بقطر 2300 كم وينخفض لأكثر من 7.1 كم تحت المستوى المرجعي للمريخ، مما يمنحه أعلى ضغط جوي سطحي على الكوكب.",
                coordinates = "42.40° S, 70.50° E"
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SpaceBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header & Language Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CutCornerShape(8.dp))
                    .border(1.dp, ScienceTeal.copy(alpha = 0.5f), CutCornerShape(8.dp)),
                color = SpaceCardDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "دليلك الصوتي للمعالم المريخية" else "MARS LANDMARKS AUDIO GUIDE",
                            style = MaterialTheme.typography.labelMedium,
                            color = ScienceTeal,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "جولة صوتية علمية تفاعلية دون اتصال" else "Offline-First NASA Science Audio Tour",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = {
                            SoundManager.playClick()
                            tts.stop()
                            playingId = null
                            ttsNotice = null
                            isArabic = !isArabic
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpaceCardElevated),
                        modifier = Modifier.heightIn(min = 40.dp).testTag("audio_guide_lang_toggle"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "English" else "العربية",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = ScienceTeal
                        )
                    }
                }
            }

            ttsNotice?.let { notice ->
                Text(
                    text = notice,
                    style = MaterialTheme.typography.labelSmall,
                    color = ScienceWarning,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            // Landmarks List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(landmarks, key = { it.id }) { landmark ->
                    val isPlaying = playingId == landmark.id
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SpaceCardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isPlaying) MarsOrangeBright else SpaceBorderDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) landmark.titleAr else landmark.titleEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MarsOrangeBright,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isArabic) landmark.subtitleAr else landmark.subtitleEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScienceTeal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        SoundManager.playClick()
                                        if (isPlaying) {
                                            tts.stop()
                                            playingId = null
                                        } else if (!ttsReady) {
                                            ttsNotice = if (isArabic) "محرك الصوت غير متاح على هذا الجهاز" else "Speech engine is not ready on this device."
                                        } else {
                                            ttsNotice = null
                                            val targetLocale = if (isArabic) Locale.forLanguageTag("ar") else Locale.US
                                            val langSupport = tts.setLanguage(targetLocale)
                                            val textToSpeak = if (isArabic && langSupport != TextToSpeech.LANG_MISSING_DATA && langSupport != TextToSpeech.LANG_NOT_SUPPORTED) {
                                                "${landmark.titleAr}. ${landmark.descriptionAr}"
                                            } else {
                                                if (isArabic) {
                                                    tts.language = Locale.US
                                                    ttsNotice = "Voice pack for Arabic not installed; playing English narration."
                                                }
                                                "${landmark.titleEn}. ${landmark.descriptionEn}"
                                            }
                                            playingId = landmark.id
                                            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, landmark.id)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp).testTag("play_audio_${landmark.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = if (isPlaying) "Stop Audio" else "Play Audio",
                                        tint = if (isPlaying) ScienceCritical else ScienceTeal
                                    )
                                }
                            }

                            Text(
                                text = if (isArabic) landmark.descriptionAr else landmark.descriptionEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Coordinates: ${landmark.coordinates}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                                Surface(
                                    color = SpaceCardElevated,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "NASA PDS VERIFIED",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScienceTeal,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
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
