package com.jabonilla.balance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Minutes per day x days per week; the weekly target is derived. */
@Composable
fun TargetFields(perDay: Int, days: Int, onPerDay: (Int) -> Unit, onDays: (Int) -> Unit) {
    @Composable
    fun stepper(text: String, value: Int, range: IntRange, step: Int, set: (Int) -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), fontSize = 17.sp)
            IconButton({ set(value - step) }, enabled = value - step >= range.first) { Icon(painterResource(R.drawable.ic_remove), "Decrease") }
            IconButton({ set(value + step) }, enabled = value + step <= range.last) { Icon(painterResource(R.drawable.ic_add), "Increase") }
        }
    }
    Column {
        stepper("$perDay min a day", perDay, 5..240, 5, onPerDay)
        stepper("$days day${if (days == 1) "" else "s"} a week", days, 1..7, 1, onDays)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Weekly target", Modifier.weight(1f), fontSize = 17.sp)
            Text((perDay * days).minutesText, fontSize = 17.sp)
        }
    }
}

@Composable
fun OnboardingScreen(store: Store) {
    var picked by remember { mutableStateOf<Category?>(null) }
    var perDay by remember { mutableIntStateOf(15) }
    var days by remember { mutableIntStateOf(5) }
    BackHandler(picked != null) { picked = null }

    Column(Modifier.fillMaxSize().background(AppBackground).safeDrawingPadding()) {
        val category = picked
        if (category == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("What do you want to grow?", fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp)
                Text("Pick just one to start. You can add more later.", color = Color.White.copy(alpha = 0.7f))
            }
            LazyVerticalGrid(
                GridCells.Adaptive(150.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(Category.growth) { c ->
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 100.dp).background(Surface, RoundedCornerShape(14.dp))
                            .clickable { picked = c }.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(painterResource(symbolIcon(c.symbol)), null, Modifier.size(28.dp), tint = sideColor(c.side))
                        Text(c.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(c.blurb, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        } else {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ picked = null }) { Icon(painterResource(R.drawable.ic_arrow_back), "Back") }
                Text("Weekly target", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(symbolIcon(category.symbol)), null, Modifier.size(24.dp), tint = sideColor(category.side))
                    Text(category.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(12.dp)) {
                    TargetFields(perDay, days, { perDay = it }, { days = it })
                }
                Text("Start with something doable. You can change it anytime.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                Text(
                    "We'll also track fun, so you can see the balance between growing and enjoying yourself.",
                    Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(14.dp)).padding(12.dp), color = Color.White.copy(alpha = 0.7f),
                )
                Button({ store.upsert(Goal(categoryID = category.id, minutesPerDay = perDay, daysPerWeek = days)) }, Modifier.fillMaxWidth()) {
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
