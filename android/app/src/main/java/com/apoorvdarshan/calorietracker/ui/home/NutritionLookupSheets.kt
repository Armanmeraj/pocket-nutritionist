package com.apoorvdarshan.calorietracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apoorvdarshan.calorietracker.models.MacroValueFormatter
import com.apoorvdarshan.calorietracker.models.NutritionNumberFormatter
import com.apoorvdarshan.calorietracker.services.NutritionCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionMatchSheet(
    match: PendingNutritionMatch,
    onSelect: (NutritionCandidate) -> Unit,
    onUseEstimate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Nutrition Match",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                match.item.lookupQuery,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            match.candidates.forEach { candidate ->
                NutritionCandidateCard(
                    candidate = candidate,
                    modifier = Modifier.clickable { onSelect(candidate) }
                )
            }
            TextButton(onClick = onUseEstimate, modifier = Modifier.align(Alignment.End)) {
                Text("Use AI Estimate")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDatabaseSearchSheet(
    results: List<NutritionCandidate>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (String, NutritionCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Database Search",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("Food or barcode") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = query.isNotBlank() && !isSearching,
                    onClick = {
                        hasSearched = true
                        onSearch(query)
                    }
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results, key = { it.id }) { candidate ->
                    NutritionCandidateCard(
                        candidate = candidate,
                        modifier = Modifier.clickable { onSelect(query, candidate) }
                    )
                }
                if (hasSearched && !isSearching && results.isEmpty()) {
                    item {
                        Text(
                            "No database matches",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionCandidateCard(
    candidate: NutritionCandidate,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listOfNotNull(candidate.brand, candidate.name).joinToString(" "),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                candidateDetail(candidate)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                candidate.nutritionDataSource.badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${(candidate.facts.calories ?: 0.0).toInt()} kcal", style = MaterialTheme.typography.labelMedium)
            Text("P ${MacroValueFormatter.string(candidate.facts.protein ?: 0.0)}g", style = MaterialTheme.typography.labelMedium)
            Text("C ${MacroValueFormatter.string(candidate.facts.carbs ?: 0.0)}g", style = MaterialTheme.typography.labelMedium)
            Text("F ${MacroValueFormatter.string(candidate.facts.fat ?: 0.0)}g", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            if (candidate.matchScore > 0.0) {
                Text("${(candidate.matchScore * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun candidateDetail(candidate: NutritionCandidate): String? {
    val parts = buildList {
        candidate.householdServingDescription?.takeIf { it.isNotBlank() }?.let(::add)
            ?: candidate.servingWeightGrams?.takeIf { it > 0 }
                ?.let { add("${NutritionNumberFormatter.quantity(it)} g") }
        candidate.dataType?.takeIf { it.isNotBlank() }?.let(::add)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" - ")
}
