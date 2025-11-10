# JBox2D Swing Demo — Spiegazione e guida

Questo documento spiega la libreria JBox2D, il funzionamento della demo, e fornisce una spiegazione dettagliata del codice contenuto in `src/main/java/JBox2Demo.java`. È scritto in italiano.

## Sommario

- Introduzione a JBox2D
- Concetti fondamentali usati nella demo
- Conversione coordinate (metri ↔ pixel)
- Struttura del codice (`JBox2Demo.java`): sezioni e spiegazioni
- Parametri fisici importanti
- Game loop e sincronizzazione
- Rendering con Swing (Graphics2D)
- Come eseguire il progetto
- Possibili miglioramenti ed estensioni

---

## Introduzione a JBox2D

JBox2D è una libreria Java che implementa il motore fisico 2D Box2D (originariamente sviluppato in C++). Box2D è usato per simulare corpi rigidi, collisioni, contatti, forze e vincoli in 2D con comportamento realistico.

Caratteristiche principali:
- Simulazione basata su corpi (Body), forme (Shape) e fixture (Fixture)
- Gestione di gravità, massa, attrito e restitution (rimbalzo)
- Robustezza per giochi e simulazioni 2D

Nella demo abbiamo usato la versione disponibile su Maven Central: `org.jbox2d:jbox2d-library:2.2.1.1`.

## Concetti fondamentali usati nella demo

- World: oggetto che contiene la simulazione fisica e gestisce l'evoluzione temporale (world.step(...)).
- Body: rappresenta un oggetto fisico (può essere STATIC, DYNAMIC, KINEMATIC).
- Fixture: collega una Shape a un Body e contiene proprietà fisiche (density, friction, restitution).
- Shape: definisce la geometria usata per il rilevamento delle collisioni (es. PolygonShape).

Nella demo abbiamo due body principali:
- `floorBody` — corpo STATIC che rappresenta il pavimento
- `cubeBody` — corpo DYNAMIC, un quadrato che "corre" orizzontalmente

## Conversione coordinate (metri ↔ pixel)

JBox2D lavora in unità fisiche (tipicamente metri). Swing usa pixel. Per avere una simulazione stabile è buona pratica mantenere le dimensioni dei corpi in metri (es. 0.5 m) e mappare con una scala costante.

Nella demo è definita una costante:

PIXELS_PER_METER = 30.0f

Conversioni:

- da pixel a metri: m = pixels / PIXELS_PER_METER
- da metri a pixel: pixels = m * PIXELS_PER_METER

Esempio: 60 pixel corrispondono a 2 metri se PIXELS_PER_METER = 30.

Matematicamente:

\[ x_{px} = x_{m} \cdot s \]
\[ x_{m} = \frac{x_{px}}{s} \]

dove s = PIXELS_PER_METER.

## Struttura del file `JBox2Demo.java` e spiegazione

Il file principale (`JBox2Demo.java`) contiene tutta la logica: creazione della GUI, inizializzazione del mondo JBox2D, creazione dei corpi, loop di aggiornamento, e rendering.

Di seguito una spiegazione sezione per sezione (riferimenti ai blocchi logici nel file):

### Costanti di configurazione

- `WINDOW_WIDTH`, `WINDOW_HEIGHT` — dimensioni della finestra in pixel.
- `PIXELS_PER_METER` — scala tra metri e pixel.
- `TARGET_FPS`, `TIME_STEP` — frame rate desiderato e passo temporale per la simulazione: TIME_STEP = 1 / TARGET_FPS.
- `VELOCITY_ITERATIONS`, `POSITION_ITERATIONS` — parametri usati da `world.step(...)` per i solver interni. Valori tipici: 6-8 per velocity, 2-3 per position.

### Creazione del mondo (initPhysics)

1. `Vec2 gravity = new Vec2(0.0f, -9.8f);`
   - Viene impostata la gravità verso il basso. Attenzione: in JBox2D Y positivo punta verso l'alto o verso il basso? In JBox2D Y positivo punta verso l'alto per convenzione usata qui è impostata la gravità negativa per simulare "giù".

2. `world = new World(gravity);`
   - Crea l'istanza del mondo fisico.

3. `createFloor();` e `createCube();`
   - Definiscono i due corpi con le relative fixture.

4. `cubeBody.setLinearVelocity(new Vec2(5.0f, 0.0f));`
   - Imposta una velocità iniziale lungo X (in metri/s) per far "correre" il cubo.

### Creazione del pavimento (createFloor)

- `BodyDef` con `type = BodyType.STATIC` e `position` impostata al centro orizzontale e una certa altezza.
- `PolygonShape` con `setAsBox(halfWidth, halfHeight)` — attenzione: `setAsBox` prende mezzo-larghezza e mezzo-altezza in metri.
- `FixtureDef` per specificare `friction` e `restitution`.

Nota: nella versione di demo il pavimento è molto largo (metà larghezza della finestra come mezzo-larghezza) e sottile.

### Creazione del cubo (createCube)

- `BodyDef` con `type = BodyType.DYNAMIC` e `position` in alto rispetto al pavimento.
- `PolygonShape` con `setAsBox(cubeSize, cubeSize)` dove `cubeSize` è la metà della dimensione del lato in metri.
- `FixtureDef` con `density`, `friction`, `restitution`.

La massa e l'inerzia del corpo sono calcolate automaticamente dalla fixture (dato `density` e la forma).

### Game Loop (startGameLoop)

La demo usa `javax.swing.Timer` che ad intervalli di `1000 / TARGET_FPS` ms esegue:

1. `world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);` — avanza la simulazione fisica.
2. `gamePanel.repaint();` — richiede il ridisegno del pannello Swing.
3. Controlli extra:
   - Se la velocità orizzontale scende sotto una soglia, la reimpostiamo per mantenere il movimento.
   - Se il cubo supera il bordo destro della finestra, viene "teletrasportato" a sinistra per ricominciare il tragitto.

Nota su Swing Timer: esegue sul thread Event Dispatch Thread (EDT). `world.step(...)` è relativamente veloce per demo; in progetti più complessi potresti voler eseguire la simulazione in un thread separato e sincronizzare i dati per il rendering.

### Rendering (GamePanel.paintComponent)

- Si ottiene un `Graphics2D` per disegnare.
- Si attivano gli hints di rendering per migliore qualità.
- Sfondo: riempiamo l'intera finestra.
- Per ogni corpo (`floorBody` e `cubeBody`) viene chiamato `drawBody(...)` che:
  - Legge `body.getPosition()` (in metri) e `body.getAngle()` (radianti).
  - Converte la posizione in pixel: x_px = metersToPixels(x_m).
  - Inverte la coordinata Y per Swing (perché la Y in Swing cresce verso il basso): y_px = WINDOW_HEIGHT - metersToPixels(y_m).
  - Usa trasformazioni `translate` e `rotate` per disegnare il rettangolo centrato.

Nota: il codice assume che la forma del corpo sia `PolygonShape` e usa i suoi vertici per calcolare la mezza larghezza e mezza altezza. Questo è sufficiente per questo demo, ma non è una soluzione generale per tutte le shape.

## Parametri fisici importanti

- density: influisce sulla massa (massa = density * area).
- friction: determina l'attrito tangenziale durante il contatto.
- restitution: il coefficiente di rimbalzo (0 = nessun rimbalzo, 1 = elastico perfetto).
- time step: passi molto grandi o irregolari rendono la simulazione instabile.
- solver iterations: più iterazioni migliorano la stabilità ai costi di performance.

## Game loop e sincronizzazione

Usare un time step fisso (come 1/60) è consigliato per stabilità. Se il rendering è più lento del time step, si può eseguire più volte `world.step(...)` per "catch up".

## Rendering e performance

- Swing è sufficiente per demo semplici. Per progetti più complessi o con molti corpi, considerare librerie grafiche accelerate (LWJGL, JavaFX con pipeline hardware, o librerie OpenGL).
- Evitare di eseguire `world.step(...)` per tempi lunghi sul EDT in applicazioni pesanti.

## Come eseguire il progetto

Prerequisiti:
- Java JDK 11+ installato (variabile `JAVA_HOME` impostata).
- Nessun Gradle richiesto perché è incluso il Gradle Wrapper (`gradlew.bat`).

Dal prompt di PowerShell nella cartella del progetto (`C:\Users\Luca\OneDrive\Desktop\Esempio di cubo`):

```powershell
# Eseguire la demo (Windows)
.\gradlew.bat run
```

Se vuoi rigenerare il wrapper dal sistema con Gradle installato globalmente puoi eseguire `gradle wrapper`, ma non è necessario.

## Possibili miglioramenti

- Separare la simulazione fisica (thread) dal rendering per non bloccare l'EDT.
- Supportare più oggetti dinamici e un sistema di gestione delle entità.
- Aggiungere input (tastiera/mouse) per controllare il cubo o applicare forze.
- Migliorare il disegno calcolando shape generiche (es. CircleShape, EdgeShape) in modo robusto.
- Aggiungere debug draw usando le funzionalità di Box2D per tracciare i contatti.

---

### Riferimenti utili

- JBox2D su Maven Central: https://search.maven.org/artifact/org.jbox2d/jbox2d-library
- Box2D manuale e concetti: https://box2d.org/

---

Se vuoi, posso aggiornare questo README per includere una spiegazione riga-per-riga del codice (commenti inline) o creare una versione più breve da includere come header del file `JBox2Demo.java`.
