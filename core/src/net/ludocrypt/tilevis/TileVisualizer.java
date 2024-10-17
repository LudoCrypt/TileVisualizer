package net.ludocrypt.tilevis;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;

import net.ludocrypt.tilevis.render.MeshConsumer;
import net.ludocrypt.tilevis.render.VertexConsumer;

public class TileVisualizer extends ApplicationAdapter {

    public static final VertexAttribute POS = new VertexAttribute(Usage.Position, 3, "Pos", 0);
    public static final VertexAttribute COL = new VertexAttribute(Usage.ColorUnpacked, 4, "Color", 0);
    public static final VertexAttribute UV = new VertexAttribute(Usage.TextureCoordinates, 2, "texCoord", 0);

    ShaderProgram shader;
    Mesh mesh;

    VertexConsumer vertexConsumer;

    SpriteBatch batch;
    Texture tileImg;
    Texture paletteImg;

    int width;
    int height;

    boolean choseInitFile = false;
    File initFile = null;
    File tileFile = null;
    int initLine = -1;
    int initLineOf = 0;
    int initLines = -1;

    Vector2 tileSize = new Vector2(7, 4);
    int bufferTiles = 1;
    int randomTiles = 1;
    boolean hasEffectColor = false;
    int layer = 1;
    int[] repeatL = new int[] { 1, 4, 1, 3, 1 };
    float[] trueRepeatL = createLayers(repeatL);

    int time = 0;
    boolean tickTimer = true;

    Vector2 mouse = new Vector2(0, 0);
    int lastMouseWidth;
    int lastMouseHeight;
    float depthScale = 10.0F;
    float cameraTilt = 2.0F;

    float backRotation = 0.0F;

    float outsideScale = 0.3F;

    @Override
    public void create() {
        shader = new ShaderProgram(Gdx.files.internal("blit.vsh"), Gdx.files.internal("blit.fsh"));

        mesh = new Mesh(false, 4, 6, POS, COL, UV);
        vertexConsumer = new MeshConsumer(mesh, true);

        batch = new SpriteBatch();
        tileImg = new Texture("crossbox wide.png");
        tileImg.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tileImg.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

        paletteImg = new Texture("palette0.png");
        paletteImg.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        paletteImg.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean keyTyped(char character) {

                if (character == 'a' || character == 'd') {

                    if (initLine != -1 && initLines != -1) {

                        if (initFile != null) {
                            int tryLine = initLine;

                            while (character == 'a' ? (tryLine > 0) : (tryLine < initLines)) {
                                tryLine += (character == 'a' ? -1 : 1);
                                File potentialTileFile = findFileOfLine(initFile, tryLine);

                                if (potentialTileFile != null) {

                                    initLine = tryLine;
                                    initLineOf = countCopiesUntil(potentialTileFile, initFile, initLine);

                                    filesDropped(new String[] { potentialTileFile.toString() });
                                    break;
                                }

                            }

                        }

                    }

                } else if (character == 'l') {
                    layer = layer + 1;

                    if (layer >= 4) {
                        layer = 1;
                    }

                } else if (character == ' ') {
                    tickTimer = !tickTimer;
                }

                return super.keyTyped(character);
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {

                if (amountY != 0) {

                    if (!Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) {

                        depthScale += depthScale * 0.1 * amountY;

                        if (depthScale == 0.0F) {
                            depthScale = 1.0F;
                        }

                    } else {
                        backRotation += 0.01 * amountY;
                    }

                }

                return true;
            }

        });
    }

    @Override
    public void render() {
        ScreenUtils.clear(1, 1, 1, 1);
        batch.begin();

        if (tickTimer) {
            time++;
        }

        if (lastMouseWidth == 0 || lastMouseHeight == 0) {
            lastMouseWidth = width;
            lastMouseHeight = height;
        }

        if (time % Math.max(Math.max(tileSize.x * 2, tileSize.y * 2), 6) == 0) {

            if (tileFile != null && initFile != null) {
                filesDropped(new String[] { tileFile.getAbsolutePath() });
            }

        }

        vertexConsumer.begin();

        float window = 1.0F;
        vertexConsumer.vertex(-window, -window, 0).color(255, 255, 255, 255).uv(0, 1).endVertex();
        vertexConsumer.vertex(window, -window, 0).color(255, 255, 255, 255).uv(1, 1).endVertex();
        vertexConsumer.vertex(window, window, 0).color(255, 255, 255, 255).uv(1, 0).endVertex();
        vertexConsumer.vertex(-window, window, 0).color(255, 255, 255, 255).uv(0, 0).endVertex();

        vertexConsumer.end();

        tileImg.bind(0);
        paletteImg.bind(1);
        shader.bind();
        shader.setUniformMatrix("projMat", new Matrix4());
        shader.setUniformi("iChannel0", 0);
        shader.setUniformi("iChannel1", 1);
        shader.setUniformf("iResolution", new Vector2(width, height));
        shader.setUniformf("iTime", (float) time / 60.0F);
        shader.setUniformf("iMouse", new Vector2(mouse.x / lastMouseWidth, mouse.y / lastMouseHeight));

        shader.setUniformf("outsideScale", outsideScale);
        shader.setUniformf("backRotation", backRotation);

        shader.setUniformf("sz", tileSize);
        shader.setUniformf("bfTiles", bufferTiles);
        shader.setUniformf("rnd", randomTiles);

        shader.setUniformf("layer", layer);
        shader.setUniformf("effectColor", hasEffectColor ? 2.0F : 0.0F);

        shader.setUniformf("depthScale", depthScale);
        shader.setUniformf("cameraTiltScale", cameraTilt);

        shader.setUniformf("repeatLSize", (float) repeatL.length);
        shader.setUniform1fv("repeatL", trueRepeatL, 0, 40);
        mesh.render(shader, GL20.GL_TRIANGLES);

        batch.end();

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            int mouseX = Gdx.input.getX();
            int mouseY = Gdx.input.getY();
            int windowMouseX = mouseX;
            int windowMouseY = Gdx.graphics.getHeight() - mouseY;
            lastMouseWidth = width;
            lastMouseHeight = height;
            mouse = new Vector2(windowMouseX, windowMouseY);
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            outsideScale -= Gdx.input.getDeltaY() * 0.001;
            outsideScale = Math.max(outsideScale, -0.999F);
        }

    }

    @Override
    public void dispose() {
        batch.dispose();
        tileImg.dispose();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.width = width;
        this.height = height;
    }

    public void filesDropped(String[] files) {

        if (files.length >= 1) {
            String filePath = files[0];

            if (filePath.endsWith(".png")) {

                if (isPNGWithSize(new File(filePath), 32, 16)) {
                    paletteImg = new Texture(new FileHandle(new File(filePath)));
                    paletteImg.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    paletteImg.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
                } else {

                    File potentialInit = new File(new File(filePath).getParent() + "/init.txt");

                    if (potentialInit.exists() || choseInitFile) {

                        if (parseInit(new File(filePath), potentialInit.exists() ? potentialInit : initFile)) {

                            initFile = potentialInit.exists() ? potentialInit : initFile;
                            tileFile = new File(filePath);
                            tileImg = new Texture(new FileHandle(tileFile));
                            tileImg.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                            tileImg.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);

                            this.initLines = countLines(initFile);

                            Gdx.graphics.setTitle("Tile Visualizer (by LudoCrypt): " + tileFile.getName());
                        }

                    }

                }

            } else if (filePath.toLowerCase(Locale.ROOT).endsWith("init.txt")) {
                initFile = new File(filePath);
                choseInitFile = true;
                this.initLines = countLines(initFile);
            }

        }

    }

    private static boolean isPNGWithSize(File file, int expectedWidth, int expectedHeight) {

        try {

            if (!isPNG(file)) {
                return false;
            }

            BufferedImage image = ImageIO.read(file);

            int width = image.getWidth();
            int height = image.getHeight();

            if (width != expectedWidth || height != expectedHeight) {
                return false;
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    private static boolean isPNG(File file) {

        try {
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                return false;
            }

            return true;
        } catch (IOException e) {
            return false;
        }

    }

    public static float[] createLayers(int[] intArray) {
        float[] floatArray = new float[40];

        for (int i = 0; i < intArray.length; i++) {
            floatArray[i] = (float) intArray[i];
        }

        return floatArray;
    }

    public static File findFileOfLine(File initFile, int c) {

        try (BufferedReader br = new BufferedReader(new FileReader(initFile))) {
            String line;
            int currentLine = 0;

            while ((line = br.readLine()) != null) {
                currentLine++;

                if (currentLine == c) {
                    Pattern namePattern = Pattern.compile(".*?\\s*#\\s*nm\\s*:\\s*\"");
                    Matcher nameMatcher = namePattern.matcher(line);

                    if (nameMatcher.find()) {
                        String startOfName = line.substring(nameMatcher.group().length());
                        String fileName = startOfName.substring(0, startOfName.indexOf("\"")) + ".png";

                        File lineFile = new File(initFile.getParent() + "/" + fileName);

                        if (lineFile.exists()) {
                            return lineFile;
                        }

                    }

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int countLines(File initFile) {
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(initFile))) {

            while (br.readLine() != null) {
                count++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }

    public static int countCopiesUntil(File tileFile, File initFile, int lineOf) {
        String line;
        int count = 0;
        int copies = 0;

        String searchString = "#nm:\"" + tileFile.getName().substring(0, tileFile.getName().length() - 4).toLowerCase(Locale.ROOT) + "\"";
        Pattern pattern = Pattern.compile(searchString);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(initFile));

            while ((line = reader.readLine()) != null) {
                count++;

                Matcher matcher = pattern.matcher(line.toLowerCase(Locale.ROOT));

                if (matcher.find()) {

                    if (count < lineOf) {
                        copies++;
                        continue;
                    }

                    reader.close();
                    return copies;
                }

            }

            reader.close();
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

    }

    public boolean parseInit(File tileFile, File initFile) {

        int count = 0;
        int copies = 0;
        String line = "";

        if (initFile != null && tileFile != null) {
            String searchString = "#nm:\"" + tileFile.getName().substring(0, tileFile.getName().length() - 4).toLowerCase(Locale.ROOT) + "\"";
            Pattern pattern = Pattern.compile(searchString);

            try {
                BufferedReader reader = new BufferedReader(new FileReader(initFile));

                while ((line = reader.readLine()) != null) {
                    count++;

                    Matcher matcher = pattern.matcher(line.toLowerCase(Locale.ROOT));

                    if (matcher.find()) {

                        if (copies < initLineOf) {
                            copies++;
                            continue;
                        }

                        reader.close();
                        throw new ArithmeticException();
                    }

                }

                reader.close();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (ArithmeticException huh) {

            }

        }

        if (line == "") {
            return false;
        }

        Pattern integerFinder = Pattern.compile("\\d+\\b");
        Pattern arrayFinder = Pattern.compile(".*?(?=\\])");

        Pattern sizeFinder = Pattern.compile("\\s*.*?#\\s*sz\\s*:\\s*point\\s*\\(");
        Matcher sizeFinderMatcher = sizeFinder.matcher(line.toLowerCase(Locale.ROOT));

        Pattern repeatLFinder = Pattern.compile("\\s*.*?#\\s*repeatL\\s*:\\s*\\[");

        Pattern bfTilesFinder = Pattern.compile("\\s*#\\s*bfTiles\\s*:\\s*\\d+");
        Pattern rndFinder = Pattern.compile("\\s*#\\s*rnd\\s*:\\s*\\d+");

        try {

            Vector2 size = new Vector2(0, 0);
            List<Integer> repeatL = new ArrayList<Integer>();
            int bfTiles = -1;
            int rnd = -1;

            if (sizeFinderMatcher.find()) {
                String sizeToFind = line.substring(sizeFinderMatcher.group().length());

                Matcher sizeIntegerFinder = integerFinder.matcher(sizeToFind.toLowerCase(Locale.ROOT));

                if (sizeIntegerFinder.find()) {
                    size.x = Integer.parseInt(sizeIntegerFinder.group());
                } else {
                    return false;
                }

                if (sizeIntegerFinder.find()) {
                    size.y = Integer.parseInt(sizeIntegerFinder.group());
                } else {
                    return false;
                }

                Matcher repeatLFinderMatcher = repeatLFinder.matcher(sizeToFind);

                if (repeatLFinderMatcher.find()) {
                    String repeatLToFind = sizeToFind.substring(repeatLFinderMatcher.group().length());
                    Matcher repeatLArrayFinderMatcher = arrayFinder.matcher(repeatLToFind);

                    if (repeatLArrayFinderMatcher.find()) {

                        for (String intLayer : repeatLArrayFinderMatcher.group().split(",")) {
                            String intItself = intLayer.replaceAll("\\s*", "");
                            repeatL.add(Integer.parseInt(intItself));
                        }

                    }

                } else {
                    repeatL.add(10);
                }

                Matcher bfTilesMatcher = bfTilesFinder.matcher(line.replaceAll("\\s*", ""));

                if (bfTilesMatcher.find()) {
                    String bf = bfTilesMatcher.group();
                    bfTiles = Integer.parseInt(bf.substring(9));

                    Matcher rndMatcher = rndFinder.matcher(line.replaceAll("\\s*", ""));

                    if (rndMatcher.find()) {
                        String random = rndMatcher.group();
                        rnd = Integer.parseInt(random.substring(5));
                    }

                }

            }

            if (size.x != 0 && size.y != 0 && rnd != -1 && bfTiles != -1 && repeatL.size() > 0) {

                this.tileSize = size;
                this.bufferTiles = bfTiles;
                this.randomTiles = rnd;
                this.repeatL = repeatL.stream().mapToInt(Integer::intValue).toArray();
                this.trueRepeatL = createLayers(this.repeatL);
                this.hasEffectColor = line.contains("\"effectColorA\"") || line.contains("\"effectColorB\"");
                this.initLine = count;

                return true;
            }

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

}
