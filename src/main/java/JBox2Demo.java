import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Demo JBox2D con Swing: un cubo che corre su un pavimento
 */
public class JBox2Demo extends JFrame {
    
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final float PIXELS_PER_METER = 30.0f;
    private static final int TARGET_FPS = 60;
    private static final float TIME_STEP = 1.0f / TARGET_FPS;
    private static final int VELOCITY_ITERATIONS = 8;
    private static final int POSITION_ITERATIONS = 3;
    
    private World world;
    private Body cubeBody;
    private Body floorBody;
    private GamePanel gamePanel;
    private BufferedImage spriteSheet;
    private BufferedImage cubeSprite;
    
    // Colore personalizzato per lo sprite (null = nessuna colorazione)
    private Color spriteTintColor = new Color(255, 100, 100); // Rosso chiaro come esempio
    
    public JBox2Demo() {
        setTitle("JBox2D Swing Demo - Cubo Che Corre");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        loadSprite();
        initPhysics();
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        setLocationRelativeTo(null);
        setVisible(true);
        
        startGameLoop();
    }
    
    private void loadSprite() {
        try {
            // Carica l'immagine dallo sprite sheet
            InputStream is = getClass().getResourceAsStream("/player_01-hd.jpeg");
            if (is == null) {
                System.err.println("ATTENZIONE: File oplayer_01-hd.jpeg non trovato in src/main/resources/");
                System.err.println("Il cubo verrà disegnato senza sprite.");
                return;
            }
            spriteSheet = ImageIO.read(is);
            
            // Estrai lo sprite specifico (coordinate e dimensioni dall'esempio CSS)
            // .sprite { background: url('imgs/player_01-hd.png') no-repeat -69px -2px; width: 60px; height: 61px; }
            cubeSprite = spriteSheet.getSubimage(69, 2, 60, 61);
            
            System.out.println("Sprite caricato correttamente: 60x61 pixel");
        } catch (IOException e) {
            System.err.println("Errore durante il caricamento dello sprite: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Errore durante l'estrazione dello sprite: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Applica un colore di tinta (tint) a un'immagine.
     * Mantiene i valori alpha originali dell'immagine.
     * 
     * @param source L'immagine sorgente
     * @param tintColor Il colore da applicare (null = nessuna colorazione)
     * @return Una nuova immagine colorata, o l'originale se tintColor è null
     */
    private BufferedImage applyTint(BufferedImage source, Color tintColor) {
        if (source == null || tintColor == null) {
            return source;
        }
        
        // Crea una nuova immagine con lo stesso tipo
        BufferedImage tinted = new BufferedImage(
            source.getWidth(), 
            source.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        // Estrai i componenti del colore di tinta (0.0 - 1.0)
        float tintR = tintColor.getRed() / 255.0f;
        float tintG = tintColor.getGreen() / 255.0f;
        float tintB = tintColor.getBlue() / 255.0f;
        
        // Applica il tint pixel per pixel
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                
                // Estrai i componenti ARGB
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                
                // Applica il tint moltiplicando i valori
                red = (int)(red * tintR);
                green = (int)(green * tintG);
                blue = (int)(blue * tintB);
                
                // Clamp i valori tra 0 e 255
                red = Math.min(255, Math.max(0, red));
                green = Math.min(255, Math.max(0, green));
                blue = Math.min(255, Math.max(0, blue));
                
                // Ricomponi il pixel
                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                tinted.setRGB(x, y, newPixel);
            }
        }
        
        return tinted;
    }
    
    /**
     * Applica un colore di tinta solo a una regione rettangolare dell'immagine.
     * 
     * @param source L'immagine sorgente
     * @param tintColor Il colore da applicare
     * @param regionX Coordinata X iniziale della regione (0 = sinistra)
     * @param regionY Coordinata Y iniziale della regione (0 = alto)
     * @param regionWidth Larghezza della regione da colorare
     * @param regionHeight Altezza della regione da colorare
     * @return Una nuova immagine con la regione colorata
     */
    private BufferedImage applyPartialTint(BufferedImage source, Color tintColor, 
                                          int regionX, int regionY, 
                                          int regionWidth, int regionHeight) {
        if (source == null || tintColor == null) {
            return source;
        }
        
        // Crea una copia dell'immagine originale
        BufferedImage tinted = new BufferedImage(
            source.getWidth(), 
            source.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        // Estrai i componenti del colore di tinta
        float tintR = tintColor.getRed() / 255.0f;
        float tintG = tintColor.getGreen() / 255.0f;
        float tintB = tintColor.getBlue() / 255.0f;
        
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                
                // Verifica se il pixel è dentro la regione da colorare
                boolean inRegion = (x >= regionX && x < regionX + regionWidth &&
                                   y >= regionY && y < regionY + regionHeight);
                
                if (inRegion) {
                    // Applica il tint a questo pixel
                    int alpha = (pixel >> 24) & 0xff;
                    int red = (int)(((pixel >> 16) & 0xff) * tintR);
                    int green = (int)(((pixel >> 8) & 0xff) * tintG);
                    int blue = (int)((pixel & 0xff) * tintB);
                    
                    // Clamp
                    red = Math.min(255, Math.max(0, red));
                    green = Math.min(255, Math.max(0, green));
                    blue = Math.min(255, Math.max(0, blue));
                    
                    pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                }
                
                tinted.setRGB(x, y, pixel);
            }
        }
        
        return tinted;
    }
    
    /**
     * Applica un colore di tinta solo ai pixel di un determinato colore (con tolleranza).
     * Utile per colorare solo specifiche parti dello sprite (es. solo la pelle, solo i vestiti).
     * 
     * @param source L'immagine sorgente
     * @param tintColor Il colore da applicare
     * @param targetColor Il colore originale da sostituire
     * @param tolerance Tolleranza per il matching del colore (0-255, consigliato 30-50)
     * @return Una nuova immagine con i colori target colorati
     */
    private BufferedImage applyColorMaskedTint(BufferedImage source, Color tintColor, 
                                               Color targetColor, int tolerance) {
        if (source == null || tintColor == null || targetColor == null) {
            return source;
        }
        
        BufferedImage tinted = new BufferedImage(
            source.getWidth(), 
            source.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        float tintR = tintColor.getRed() / 255.0f;
        float tintG = tintColor.getGreen() / 255.0f;
        float tintB = tintColor.getBlue() / 255.0f;
        
        int targetR = targetColor.getRed();
        int targetG = targetColor.getGreen();
        int targetB = targetColor.getBlue();
        
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                
                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                
                // Calcola la distanza dal colore target
                int diffR = Math.abs(red - targetR);
                int diffG = Math.abs(green - targetG);
                int diffB = Math.abs(blue - targetB);
                
                // Se il pixel è simile al colore target, applica il tint
                if (diffR <= tolerance && diffG <= tolerance && diffB <= tolerance) {
                    red = (int)(red * tintR);
                    green = (int)(green * tintG);
                    blue = (int)(blue * tintB);
                    
                    red = Math.min(255, Math.max(0, red));
                    green = Math.min(255, Math.max(0, green));
                    blue = Math.min(255, Math.max(0, blue));
                    
                    pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                }
                
                tinted.setRGB(x, y, pixel);
            }
        }
        
        return tinted;
    }
    
    private void initPhysics() {
        // Crea il mondo JBox2D con gravità verso il basso
        Vec2 gravity = new Vec2(0.0f, -9.8f);
        world = new World(gravity);
        
        // Crea il pavimento (corpo statico)
        createFloor();
        
        // Crea il cubo (corpo dinamico)
        createCube();
        
        // Applica velocità iniziale al cubo per farlo correre
        cubeBody.setLinearVelocity(new Vec2(5.0f, 0.0f));
    }
    
    private void createFloor() {
        // Definizione del corpo statico per il pavimento
        BodyDef floorDef = new BodyDef();
        floorDef.type = BodyType.STATIC;
        floorDef.position.set(
            pixelsToMeters(WINDOW_WIDTH / 2.0f),
            pixelsToMeters(50.0f)
        );
        
        floorBody = world.createBody(floorDef);
        
        // Forma del pavimento: rettangolo largo e sottile
        PolygonShape floorShape = new PolygonShape();
        floorShape.setAsBox(
            pixelsToMeters(WINDOW_WIDTH / 2.0f),
            pixelsToMeters(20.0f)
        );
        
        // Fixture del pavimento
        FixtureDef floorFixture = new FixtureDef();
        floorFixture.shape = floorShape;
        floorFixture.friction = 0.5f;
        floorFixture.restitution = 0.3f;
        
        floorBody.createFixture(floorFixture);
    }
    
    private void createCube() {
        // Definizione del corpo dinamico per il cubo
        BodyDef cubeDef = new BodyDef();
        cubeDef.type = BodyType.DYNAMIC;
        cubeDef.position.set(
            pixelsToMeters(100.0f),
            pixelsToMeters(300.0f)
        );
        
        cubeBody = world.createBody(cubeDef);
        
        // Forma del cubo: quadrato
        PolygonShape cubeShape = new PolygonShape();
        float cubeSize = pixelsToMeters(40.0f);
        cubeShape.setAsBox(cubeSize, cubeSize);
        
        // Fixture del cubo
        FixtureDef cubeFixture = new FixtureDef();
        cubeFixture.shape = cubeShape;
        cubeFixture.density = 1.0f;
        cubeFixture.friction = 0.3f;
        cubeFixture.restitution = 0.4f;
        
        cubeBody.createFixture(cubeFixture);
    }
    
    private void startGameLoop() {
        Timer timer = new Timer(1000 / TARGET_FPS, e -> {
            // Aggiorna la fisica
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            
            // Ridisegna
            gamePanel.repaint();
            
            // Mantieni il cubo in movimento se rallenta troppo
            Vec2 velocity = cubeBody.getLinearVelocity();
            if (Math.abs(velocity.x) < 3.0f) {
                cubeBody.setLinearVelocity(new Vec2(5.0f, velocity.y));
            }
            
            // Teletrasporta il cubo se esce dallo schermo
            Vec2 position = cubeBody.getPosition();
            if (metersToPixels(position.x) > WINDOW_WIDTH + 50) {
                cubeBody.setTransform(new Vec2(pixelsToMeters(-50.0f), position.y), cubeBody.getAngle());
                cubeBody.setLinearVelocity(new Vec2(5.0f, velocity.y));
            }
        });
        timer.start();
    }
    
    private float pixelsToMeters(float pixels) {
        return pixels / PIXELS_PER_METER;
    }
    
    private float metersToPixels(float meters) {
        return meters * PIXELS_PER_METER;
    }
    
    // Pannello per il rendering con Swing
    private class GamePanel extends JPanel {
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            
            // Abilita antialiasing per grafica migliore
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Sfondo
            g2d.setColor(new Color(240, 248, 255));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Disegna il pavimento
            drawBody(g2d, floorBody, new Color(101, 67, 33), false);
            
            // Disegna il cubo (con sprite se disponibile)
            drawBody(g2d, cubeBody, new Color(220, 20, 60), true);
            
            // Disegna informazioni di debug
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            Vec2 cubeVel = cubeBody.getLinearVelocity();
            g2d.drawString(String.format("Velocità: %.2f m/s", cubeVel.length()), 10, 20);
            Vec2 cubePos = cubeBody.getPosition();
            g2d.drawString(String.format("Posizione: (%.2f, %.2f)", cubePos.x, cubePos.y), 10, 40);
        }
        
        private void drawBody(Graphics2D g2d, Body body, Color color, boolean useSprite) {
            Vec2 position = body.getPosition();
            float angle = body.getAngle();
            
            // Ottieni la forma del corpo (assumiamo PolygonShape)
            PolygonShape shape = (PolygonShape) body.getFixtureList().getShape();
            
            // Per un rettangolo (setAsBox crea un rettangolo centrato)
            float halfWidth = metersToPixels(shape.m_vertices[1].x);
            float halfHeight = metersToPixels(shape.m_vertices[2].y);
            
            // Converti coordinate JBox2D in coordinate Swing
            float x = metersToPixels(position.x);
            float y = WINDOW_HEIGHT - metersToPixels(position.y); // Inverti Y per Swing
            
            // Salva lo stato di trasformazione corrente
            var oldTransform = g2d.getTransform();
            
            // Applica trasformazioni per posizione e rotazione
            g2d.translate(x, y);
            g2d.rotate(-angle); // Ruota in senso opposto per Swing
            
            // Disegna lo sprite se disponibile e richiesto, altrimenti rettangolo colorato
            if (useSprite && cubeSprite != null) {
                BufferedImage spriteToRender = cubeSprite;
                
                // Ottieni le dimensioni originali dello sprite per calcoli adattivi
                int origSpriteWidth = cubeSprite.getWidth();
                int origSpriteHeight = cubeSprite.getHeight();
                
                // Calcola i margini per la parte esterna (20% del bordo per la parte verde)
                int marginX = (int)(origSpriteWidth * 0.2);  // 20% margine orizzontale
                int marginY = (int)(origSpriteHeight * 0.2); // 20% margine verticale
                
                // Calcola le dimensioni della parte centrale
                int centerX = marginX;
                int centerY = marginY;
                int centerWidth = origSpriteWidth - (2 * marginX);
                int centerHeight = origSpriteHeight - (2 * marginY);
                
                // PASSO 1: Colora tutta la parte esterna di verde
                spriteToRender = applyTint(cubeSprite, new Color(100, 255, 100));
                
                // PASSO 2: Colora la parte centrale di azzurro (sovrascrive il verde al centro)
                spriteToRender = applyPartialTint(spriteToRender, 
                                                  new Color(100, 200, 255),  // Azzurro
                                                  centerX, centerY,          // Posizione centrale
                                                  centerWidth, centerHeight); // Dimensioni centrali
                
                // --- ALTERNATIVE COMMENTATE (prova decommentando una di queste) ---
                
                // ALTERNATIVA 1: Bordo verde più stretto (10% invece di 20%)
                // int smallMargin = (int)(origSpriteWidth * 0.1);
                // spriteToRender = applyTint(cubeSprite, new Color(100, 255, 100));
                // spriteToRender = applyPartialTint(spriteToRender, new Color(100, 200, 255),
                //                                   smallMargin, smallMargin,
                //                                   origSpriteWidth - 2*smallMargin, origSpriteHeight - 2*smallMargin);
                
                // ALTERNATIVA 2: Tre zone - bordo verde, anello intermedio giallo, centro azzurro
                // spriteToRender = applyTint(cubeSprite, new Color(100, 255, 100));  // Verde esterno
                // int midMargin = (int)(origSpriteWidth * 0.15);
                // spriteToRender = applyPartialTint(spriteToRender, new Color(255, 255, 100),  // Giallo intermedio
                //                                   midMargin, midMargin, 
                //                                   origSpriteWidth - 2*midMargin, origSpriteHeight - 2*midMargin);
                // int innerMargin = (int)(origSpriteWidth * 0.3);
                // spriteToRender = applyPartialTint(spriteToRender, new Color(100, 200, 255),  // Azzurro centro
                //                                   innerMargin, innerMargin,
                //                                   origSpriteWidth - 2*innerMargin, origSpriteHeight - 2*innerMargin);
                
                // ALTERNATIVA 3: Solo metà verticale - verde sopra, azzurro sotto
                // spriteToRender = applyPartialTint(cubeSprite, new Color(100, 255, 100), 
                //                                   0, 0, origSpriteWidth, origSpriteHeight/2);
                // spriteToRender = applyPartialTint(spriteToRender, new Color(100, 200, 255),
                //                                   0, origSpriteHeight/2, origSpriteWidth, origSpriteHeight/2);
                
                // Disegna lo sprite centrato e scalato per adattarsi al corpo
                int spriteWidth = (int)(halfWidth * 2);
                int spriteHeight = (int)(halfHeight * 2);
                g2d.drawImage(spriteToRender, 
                    -(int)halfWidth, -(int)halfHeight,
                    spriteWidth, spriteHeight, 
                    null);
                
                // Bordo opzionale attorno allo sprite
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.setStroke(new BasicStroke(1));
                Rectangle2D rect = new Rectangle2D.Float(
                    -halfWidth, -halfHeight,
                    halfWidth * 2, halfHeight * 2
                );
                g2d.draw(rect);
            } else {
                // Disegna il rettangolo colorato (fallback o per altri corpi)
                g2d.setColor(color);
                Rectangle2D rect = new Rectangle2D.Float(
                    -halfWidth, -halfHeight,
                    halfWidth * 2, halfHeight * 2
                );
                g2d.fill(rect);
                
                // Bordo
                g2d.setColor(color.darker());
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(rect);
            }
            
            // Ripristina lo stato di trasformazione
            g2d.setTransform(oldTransform);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(JBox2Demo::new);
    }
}
