# 🎬 Sistema Recomendador de Películas Inteligente

## 🧩 Descripción general
Este proyecto, desarrollado en **Java**, implementa un **sistema inteligente de recomendación de películas**.  
Permite al usuario:
- Buscar películas por **género**.  
- Aplicar filtros de **valoración mínima (rating)**.  
- Obtener **recomendaciones automáticas basadas en el clima actual de Madrid**, integrando datos de una **API meteorológica** y otra de películas.

Según el tipo de clima detectado (despejado, lluvia, nieve, etc.), el sistema asocia **géneros más apropiados** y muestra títulos sugeridos, combinando técnicas de razonamiento simbólico con acceso a información dinámica de internet.

---

## 🎯 Objetivos principales
- Desarrollar un **agente inteligente** capaz de ofrecer recomendaciones personalizadas.  
- Integrar **múltiples APIs externas** (clima y películas) dentro de un entorno Java.  
- Implementar una **interfaz gráfica interactiva (Swing)** para facilitar la búsqueda y filtrado.  
- Aplicar **técnicas de razonamiento contextual mediante agentes** (asociación clima–género).

---

## 🧱 Estructura del proyecto
| Módulo / Carpeta | Descripción destacada |
|------------------|-----------------------|
| **`src/es/upm/filmrecommender`** | Código fuente principal del sistema y de la interfaz gráfica. |
| **`jars/`** | Librerías externas necesarias: `gson-2.10.1.jar`, `jade.jar`. |
| **`MainLauncher.java`** | Clase principal que lanza el sistema y los agentes inteligentes. |

---

## ⚙️ Tecnologías utilizadas
- **Lenguaje:** Java  
- **Librerías:** Gson (manejo de JSON), JADE (arquitectura multiagente)  
- **APIs:** OpenWeatherMap + TheMovieDB  
- **Interfaz:** Swing  
- **Entorno de desarrollo:** Eclipse IDE  

---

## 🧮 Flujo de funcionamiento
```mermaid
graph TD
A[API de Clima] --> B[Identificación del tipo de clima]
B --> C[Asignación de géneros recomendados]
C --> D[Consulta a la API de Películas]
D --> E[Filtrado por rating y visualización en la interfaz gráfica]
