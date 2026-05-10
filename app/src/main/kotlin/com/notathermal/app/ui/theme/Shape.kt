package com.notathermal.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * More rounded corners than Material default. Aligns with Material 3
 * Expressive feel — large containers feel pill-like, small elements still
 * remain readable. Adjust here to retune the whole app.
 */
internal val NotaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
