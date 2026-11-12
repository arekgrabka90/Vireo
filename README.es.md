# VIREO

**Tu entretenimiento. Una sola experiencia.**

VIREO es una aplicación multimedia que reúne televisión en directo, películas y series en un entorno de entretenimiento coherente. El proyecto se centra en ofrecer una experiencia de visualización de calidad, una interfaz clara, reproducción fiable y una base flexible para seguir evolucionando.

## Visión

VIREO quiere que descubrir y ver contenido sea sencillo y consistente. Los canales en directo, las películas y las series se presentan en una sola aplicación, con perfiles personalizados y una experiencia de reproducción unificada.

El producto se desarrolla con identidad, lenguaje visual y arquitectura técnica propios.

## Contenido y entretenimiento

### TV en directo
- Acceso a canales de televisión mediante fuentes IPTV compatibles.
- Información y logotipos de los canales cuando la fuente los proporciona.
- Guía electrónica de programación (EPG), con programas actuales y próximos.
- Información relevante del programa disponible durante la reproducción.

### Películas
- Catálogo de películas enriquecido con metadatos de TMDB.
- Títulos, descripciones, años de estreno, géneros, valoraciones y duración.
- Imágenes y logotipos cuando estén disponibles.
- Reproducción desde fuentes VOD compatibles.

### Series
- Catálogos organizados por temporadas y episodios.
- Información, descripciones e imágenes específicas de cada episodio cuando estén disponibles.
- Navegación clara entre series, temporadas y episodios.
- Reproducción desde fuentes VOD compatibles.

## Experiencia de reproducción premium

VIREO se está construyendo sobre una base de reproducción común para los tipos de contenido compatibles. La interfaz muestra información contextual en pantalla sin ocultar el vídeo.

La experiencia se centra en:
- Una visualización en pantalla limpia y transparente.
- Controles e información adaptados al contenido.
- Indicaciones claras del estado de reproducción y de la línea de tiempo cuando existen datos fiables.
- Un comportamiento coherente en TV en directo, películas y series.
- Estabilidad y gestión predecible de las sesiones.

## Perfiles y personalización

VIREO está diseñada para admitir perfiles de usuario individuales. El objetivo a largo plazo es organizar las preferencias y los ajustes por perfil, para que cada persona pueda adaptar su experiencia.

## Diseñada para Android

El proyecto se desarrolla para el ecosistema Android, con una experiencia adaptable prevista para teléfonos, tabletas y Android TV. La interfaz y la interacción se ajustarán a las características de cada dispositivo.

## Arquitectura y tecnología

VIREO utiliza una arquitectura modular pensada para facilitar el mantenimiento y permitir el crecimiento continuo del proyecto.

Áreas tecnológicas principales:
- **Android** — plataforma de la aplicación.
- **Jetpack Compose** — interfaz declarativa y moderna.
- **Media3** — reproducción multimedia e integración de sesiones.
- **TMDB** — metadatos de películas y series.
- **Fuentes compatibles con Xtream** — catálogos compatibles de TV en directo y VOD.
- **EPG/XMLTV** — información de programación de televisión en directo.

Cada fuente tiene una función definida: los proveedores de catálogos y streams ofrecen contenido y fuentes de reproducción, TMDB enriquece la información de películas y series, y EPG proporciona horarios y descripciones de los programas de TV en directo.

## Fiabilidad y calidad

La fiabilidad es una prioridad fundamental. Las compilaciones y pruebas automatizadas ayudan a detectar regresiones y verificar los cambios.

Las prioridades incluyen:
- Reproducción y gestión de sesiones predecibles.
- Separación clara de las responsabilidades de las fuentes de datos.
- Gestión adecuada de metadatos e imágenes no disponibles.
- Código mantenible y cambios específicos.
- Verificación automatizada mediante el proceso de compilación.

## Hoja de ruta

VIREO está en desarrollo activo. Las funciones se incorporan por etapas, dando prioridad a una base estable antes de ampliar el producto.

Las líneas de trabajo previstas incluyen:
- Reproductor y experiencia de visualización en pantalla.
- TV en directo, EPG y gestión de canales.
- Catálogos de películas y series.
- Ajustes por perfil y personalización.
- Diseños adaptables a distintos dispositivos Android.
- Rendimiento, fiabilidad y acabado general del producto.

## Estado del proyecto

VIREO sigue en desarrollo. Algunas funciones ya están implementadas, mientras que otras están planificadas o en proceso de mejora. El repositorio y su historial reflejan el estado actual del proyecto.

## Filosofía del proyecto

VIREO parte de una idea sencilla: reunir distintas formas de entretenimiento en una experiencia única, coherente y bien diseñada.

**VIREO — Tu entretenimiento. Una sola experiencia.**
