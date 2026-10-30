# PictoHabla

Plataforma de Comunicación Aumentativa y Alternativa (CAA) para Android, desarrollada con Kotlin y Jetpack Compose. Diseñada para niños y personas con dificultades de comunicación verbal, diversidad funcional o en situación de dependencia.

## Características Principales

- **Comunicación Aumentativa y Alternativa (CAA):** Construcción visual e intuitiva de frases mediante pictogramas.
- **Offline-First:** Persistencia completa en base de datos local Room SQLite sin requerir conexión a internet.
- **Síntesis de Voz (TTS):** Reproducción fluida y natural en múltiples idiomas (Español, Inglés, Francés, Alemán, Italiano).
- **Tableros Personalizados:** Sistema multi-tablero con orden, número de columnas y vocabulario personalizables.
- **Pictogramas Propios:** Creación y almacenamiento de pictogramas personalizados con fotografías locales.
- **Accesibilidad Integral:** Modo de Alto Contraste (OLED), cuadrícula grande adaptable, soporte semántico para TalkBack y feedback háptico.
- **Clave de Fitzgerald Modificada:** Categorización clínica estandarizada por colores.
- **Zona para Cuidadores:** Protección de ajustes y administración mediante barrera matemática (Adult Gate).

## Tecnologías

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Persistencia:** Room SQLite & SharedPreferences
- **Voz:** Android Text-to-Speech (TTS)
- **Arquitectura:** Modularizada (Clean Architecture + Repository Pattern)
