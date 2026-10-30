# 💬 PictoHabla

### Comunícate a tu manera.

**PictoHabla** es una aplicación Android de Comunicación Aumentativa y Alternativa (CAA) diseñada para ayudar a niños y personas con dificultades de comunicación verbal a expresar lo que sienten, necesitan y quieren decir.

A través de pictogramas, cualquier usuario puede construir frases de una forma sencilla e intuitiva y reproducirlas mediante voz.

La idea detrás del proyecto es muy sencilla: **todo el mundo merece tener una forma de comunicarse y ser escuchado.**

---

## 🌱 ¿Por qué PictoHabla?

La comunicación es algo que muchas veces damos por hecho. Sin embargo, para algunas personas expresar algo tan cotidiano como «Tengo hambre», «Quiero jugar» o «Necesito ayuda» puede suponer una gran dificultad.

PictoHabla nace con la intención de hacer esa comunicación un poco más fácil.

No busca sustituir la comunicación de ninguna persona, sino ofrecer una herramienta que facilite expresarse, compartir necesidades y conectar con los demás.

Queremos que sea una aplicación accesible, sencilla y que pueda adaptarse a cada usuario.

## ✨ ¿Qué puedes hacer con PictoHabla?

### 🗣️ Comunicar mediante pictogramas
- Construir frases seleccionando diferentes pictogramas.
- Escuchar las frases mediante síntesis de voz (TTS).
- Eliminar palabras individualmente o borrar una frase completa.
- Consultar y recuperar expresiones anteriores.

### 🎨 Tableros personalizados
- Crear diferentes tableros de comunicación.
- Añadir y organizar pictogramas.
- Modificar el orden y la distribución.
- Adaptar el número de columnas.
- Crear pictogramas propios utilizando fotografías del dispositivo.

### ⭐ Favoritos e historial
- Guardar pictogramas favoritos.
- Acceder rápidamente a las palabras más utilizadas.
- Consultar el historial de expresiones.
- Recuperar frases anteriores y volver a reproducirlas.

### ♿ Accesibilidad
- Modo de alto contraste.
- Pictogramas de mayor tamaño.
- Elementos táctiles amplios.
- Compatibilidad con TalkBack.
- Feedback háptico configurable.
- Organización basada en la Clave de Fitzgerald Modificada.

### 📶 Funcionamiento sin Internet
Una de las ideas fundamentales de PictoHabla es que las funciones esenciales de comunicación estén disponibles incluso sin conexión.

El catálogo local, los tableros, los favoritos, el historial y la construcción de frases se gestionan en el propio dispositivo.

La síntesis de voz utiliza el motor TTS de Android y depende de los idiomas y voces disponibles en el dispositivo.

---

## 🛠️ Tecnologías utilizadas

El proyecto está desarrollado utilizando tecnologías del ecosistema Android:

| Tecnología | Uso |
|---|---|
| Kotlin | Lenguaje principal |
| Jetpack Compose | Interfaz de usuario |
| Material 3 | Componentes visuales |
| Room SQLite | Persistencia local |
| SharedPreferences | Preferencias de usuario |
| Android Text-to-Speech | Síntesis de voz |
| Gradle | Construcción del proyecto |
| Git y GitHub | Control de versiones |

La aplicación sigue una arquitectura modularizada, separando la interfaz, los modelos, el acceso a datos y la lógica de las diferentes funcionalidades.

---

## 🧩 Diseño pensado para cada persona

No todas las personas se comunican de la misma manera ni necesitan los mismos recursos.

Por eso, PictoHabla apuesta por la personalización: cada usuario puede disponer de sus propios tableros y adaptar la distribución de los pictogramas a sus necesidades.

La organización visual toma como referencia la Clave de Fitzgerald Modificada, utilizando colores para facilitar la identificación de diferentes categorías gramaticales y semánticas.

La accesibilidad no es un añadido: forma parte de la idea principal del proyecto.

---

## 🚀 Hoja de ruta

PictoHabla sigue en desarrollo. Estas son algunas de las funcionalidades que queremos incorporar:

- [x] Arquitectura inicial y modularización.
- [x] Sistema de diseño accesible.
- [x] Persistencia local con Room.
- [x] Construcción de frases mediante pictogramas.
- [x] Síntesis de voz.
- [x] Favoritos e historial.
- [x] Catálogo de pictogramas.
- [x] Creación de pictogramas personalizados.
- [x] Múltiples tableros de comunicación.
- [x] Adaptación visual para teléfonos y tablets.
- [x] Actividades educativas en el Modo Aprender.
- [x] Backend propio, PostgreSQL y autenticación de profesores.
- [ ] Panel web para profesores y cuidadores.
- [ ] Gestión de aulas.
- [ ] Comunicación en tiempo real mediante el Modo Aula.

Estas funcionalidades se irán incorporando progresivamente, sin perder de vista el objetivo principal: facilitar la comunicación.

---

## 💻 Instalación y desarrollo

¿Quieres probar el proyecto o echar un vistazo al código?

1. Clona el repositorio:

```bash
git clone https://github.com/Jacmanzano/PictoHabla.git
```

2. Abre la carpeta del proyecto con Android Studio.

3. Espera a que Gradle sincronice las dependencias.

4. Ejecuta la aplicación en un emulador o dispositivo Android compatible.

### Compilar desde terminal

Para ejecutar las pruebas unitarias:

```bash
./gradlew testDebugUnitTest
```

Para generar la versión de depuración:

```bash
./gradlew assembleDebug
```

En Windows puedes utilizar `gradlew.bat` en lugar de `./gradlew`.

---

## ❤️ La filosofía del proyecto

PictoHabla quiere seguir tres principios:

**Accesibilidad:** la comunicación debe ser lo más sencilla posible.

**Autonomía:** cada persona debe poder adaptar su tablero a su propia forma de comunicarse.

**Privacidad:** las funciones esenciales deben poder utilizarse localmente, evitando depender de servicios externos innecesarios.

El objetivo es mantener el proyecto gratuito, sin publicidad ni suscripciones, y estudiar su publicación como software abierto.

---

## 👨‍💻 Sobre el proyecto

PictoHabla es un proyecto personal desarrollado con ilusión por aprender, construir algo útil y explorar cómo la tecnología puede ayudar a mejorar la vida de las personas.

Detrás hay muchas horas de programación, pruebas, diseño y aprendizaje.

No pretende ser una solución universal ni sustituir el trabajo de profesionales de la comunicación. Es un proyecto en evolución que aspira a convertirse en una herramienta de apoyo real.

**Porque comunicarse no debería ser un privilegio.**

---

⭐ Si te parece interesante el proyecto, puedes darle una estrella al repositorio y ayudar a que llegue a más personas.

**GitHub:** [Jacmanzano/PictoHabla](https://github.com/Jacmanzano/PictoHabla)