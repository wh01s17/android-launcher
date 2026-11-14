# Reglas de R8 de la app (plan.md §7 Hito 11). El código no usa reflexión: servicios, actividades y
# receptores los conserva el manifiesto, y las bibliotecas traen sus propias reglas.

# Conserva números de línea en las trazas (se descifran con el mapping.txt de cada release).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
