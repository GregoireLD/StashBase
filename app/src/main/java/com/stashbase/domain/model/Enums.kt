package com.stashbase.domain.model

enum class MaterialType(val label: String) {
    FABRIC("Tissu"),
    THREAD("Fil"),
    YARN("Laine / Fil à tricoter"),
    HARDWARE("Mercerie"),
    WOOD("Bois"),
    LEATHER("Cuir"),
    PAPER("Papier / Carton"),
    RESIN("Résine"),
    PAINT("Peinture"),
    OTHER("Autre")
}

enum class MaterialUnit(val label: String, val abbreviation: String) {
    METER("Mètre", "m"),
    CENTIMETER("Centimètre", "cm"),
    MILLIMETER("Millimètre", "mm"),
    GRAM("Gramme", "g"),
    KILOGRAM("Kilogramme", "kg"),
    PIECE("Pièce", "pcs"),
    SKEIN("Pelote / Écheveau", "éch."),
    SHEET("Feuille", "f."),
    LITER("Litre", "L"),
    MILLILITER("Millilitre", "mL"),
    ROLL("Rouleau", "roul.")
}

enum class ToolType(val label: String) {
    NEEDLE("Aiguille"),
    CROCHET_HOOK("Crochet"),
    SCISSORS("Ciseaux"),
    DRILL_BIT("Foret"),
    DRILL("Perceuse"),
    CUTTING_MAT("Tapis de découpe"),
    RULER("Règle / Gabarit"),
    IRON("Fer à repasser"),
    SEWING_MACHINE("Machine à coudre"),
    BRUSH("Pinceau"),
    KNIFE("Couteau / Cutter"),
    CLAMP("Serre-joint / Pince"),
    OTHER("Autre")
}

enum class ToolCondition(val label: String) {
    NEW("Neuf"),
    GOOD("Bon état"),
    WORN("Usé"),
    BROKEN("Cassé / HS")
}

enum class ProjectStatus(val label: String) {
    IDEA("Idée"),
    PLANNED("Planifié"),
    IN_PROGRESS("En cours"),
    ON_HOLD("En pause"),
    COMPLETED("Terminé"),
    ARCHIVED("Archivé")
}
