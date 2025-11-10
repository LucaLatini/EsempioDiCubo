import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

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
    
    public JBox2Demo() {
        setTitle("JBox2D Swing Demo - Cubo Che Corre");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        initPhysics();
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        setLocationRelativeTo(null);
        setVisible(true);
        
        startGameLoop();
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
            drawBody(g2d, floorBody, new Color(101, 67, 33));
            
            // Disegna il cubo
            drawBody(g2d, cubeBody, new Color(220, 20, 60));
            
            // Disegna informazioni di debug
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            Vec2 cubeVel = cubeBody.getLinearVelocity();
            g2d.drawString(String.format("Velocità: %.2f m/s", cubeVel.length()), 10, 20);
            Vec2 cubePos = cubeBody.getPosition();
            g2d.drawString(String.format("Posizione: (%.2f, %.2f)", cubePos.x, cubePos.y), 10, 40);
        }
        
        private void drawBody(Graphics2D g2d, Body body, Color color) {
            Vec2 position = body.getPosition();
            float angle = body.getAngle();
            
            // Ottieni la forma del corpo (assumiamo PolygonShape)
            PolygonShape shape = (PolygonShape) body.getFixtureList().getShape();
            
            // Trasforma le coordinate e disegna
            g2d.setColor(color);
            
            // Per un rettangolo (setAsBox crea un rettangolo centrato)
            float halfWidth = metersToPixels(shape.m_vertices[1].x);
            float halfHeight = metersToPixels(shape.m_vertices[2].y);
            
            // Converti coordinate JBox2D in coordinate Swing
            float x = metersToPixels(position.x);
            float y = WINDOW_HEIGHT - metersToPixels(position.y); // Inverti Y per Swing
            
            // Salva lo stato di trasformazione
            g2d.translate(x, y);
            g2d.rotate(-angle); // Ruota in senso opposto per Swing
            
            // Disegna il rettangolo centrato
            Rectangle2D rect = new Rectangle2D.Float(
                -halfWidth, -halfHeight,
                halfWidth * 2, halfHeight * 2
            );
            g2d.fill(rect);
            
            // Bordo
            g2d.setColor(color.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(rect);
            
            // Ripristina lo stato di trasformazione
            g2d.rotate(angle);
            g2d.translate(-x, -y);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(JBox2Demo::new);
    }
}
