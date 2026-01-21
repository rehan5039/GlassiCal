package com.rubex.glassical.ui.theme

import androidx.compose.ui.graphics.Color

// Realme Inspired Colors - Refined for Glassmorphism
// The equals button is a distinctive orange/brown gradient in some themes, or flat orange.
val RealmeOrange = Color(0xFFEA8A3D) 
val RealmeOrangePressed = Color(0xFFC97330)

// Backgrounds
val RealmeDarkBg = Color(0xFF000000)
val RealmeLightBg = Color(0xFFF2F2F7)

// Text
val TextWhite = Color(0xFFFFFFFF)
val TextBlack = Color(0xFF000000)
val TextSecondary = Color(0xFF8E8E93) // For AC, %, etc if needed, or previous operation

// Button Base Colors (Glass tint)
// We use a white tint with very low alpha for dark mode glass
val GlassButtonDark = Color(0xFF2C2C2E) // Fallback opaque
val GlassTintDark = Color.White.copy(alpha = 0.12f)
val GlassTintLight = Color.Black.copy(alpha = 0.05f)

// Button Borders
val GlassBorderDark = Color.White.copy(alpha = 0.10f)
val GlassBorderLight = Color.Black.copy(alpha = 0.05f)

// Operator/Special Button Tints
val OperatorGlassDark = Color(0xFF3A3A3C) // Slightly lighter for operators
val ActionGlassDark = Color(0xFF505050) // For top row if different? Usually just slightly distinct.