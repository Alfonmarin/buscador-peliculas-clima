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
| Carpeta / Módulo | Descripción destacada |
|------------------|-----------------------|
| **`src/es/upm/filmrecommender/agents`** | Contiene los **agentes principales del sistema**, incluyendo: <br>• `AgenteRecomendador.java` – coordina las peticiones y respuestas entre los demás agentes. <br>• `AgenteInterfazUsuario.java` – gestiona la comunicación con la interfaz gráfica. <br>• `AgenteInteligente.java` – aplica la lógica de recomendación según el clima. <br>• `AgenteAdquisicionDatosPeliculas.java` – obtiene datos desde la API de películas. |
| **`src/es/upm/filmrecommender/data`** | Gestión de los datos y preferencias del usuario: <br>• `Movie.java`, `UserPreferences.java`, `HistorialVistosManager.java`. |
| **`src/es/upm/filmrecommender/gui`** | Interfaz gráfica del sistema: <br>• `RecommenderGui.java` implementa la ventana principal para búsqueda y visualización de recomendaciones. |
| **`src/es/upm/filmrecommender/utils`** | Clases auxiliares y de conexión con APIs: <br>• `ClimaClient.java` (API clima), `TMDBApiClient.java` (API películas). |
| **`MainLauncher.java`** | Punto de entrada del programa; inicializa los agentes y lanza la interfaz. |
| **`jars/`** | Librerías externas necesarias: `gson-2.10.1.jar` y `jade.jar`. |
| **`instrucciones_SSII.pdf`** | Documento con las instrucciones oficiales de ejecución del proyecto. |


---

## ⚙️ Tecnologías utilizadas
- **Lenguaje:** Java  
- **Librerías:** Gson (manejo de JSON), JADE (arquitectura multiagente)  
- **APIs:** OpenWeatherMap + TheMovieDB  
- **Interfaz:** Swing  
- **Entorno de desarrollo:** Eclipse IDE  

---

## 🤖 Funcionamiento de agentes
El sistema se basa en una **arquitectura multi-agente** implementada con **JADE**, donde cada agente tiene un rol definido y se comunican mediante mensajes **FIPA-ACL** (REQUEST / INFORM / QUERY).

```mermaid
graph TD
A[AgenteInterfazUsuario (IA)] -->|REQUEST| B[AgenteRecomendador]
B -->|INFORM| A
B -->|REQUEST| C[AgenteAdquisicionDatosPeliculas (MDAA)]
C -->|INFORM| B
B -->|QUERY| D[AgenteInteligente]
D -->|INFORM| B
