# TileVisualizer
 Visualize Tiles for Rain World! If youre looking through the code, its not optimized or anything. I just tried getting it to work lol.
 
**Using the Visualizer:**

By default, the visualizer will display the 'crossbox wide' tile with the outskirt palette. To view your own tiles, click and drag a file into the program window. It will attempt to find the init file in the folder, if one does not exist, you can drag in your own. If nothing goes wrong, you'll see your tile. To use your own palette, simply drag in any 32x16 image. It assumes all 32x16 images are treated as palettes. The title of the window will show the currently displaying tile.

**Keybinds:**

- Press 'a' and 'd' to go to the previous/next tile in the init.txt.
- Press 'l' to change the view layer (changes the palette).
- Press space to pause the random variation timer. (If a tile has rnd variations, it will cycle every second).

**Camera Control:**

- Click and drag to move around the perspective.
- Scroll to adjust the strength of the parallax.
- Right-click and drag your mouse up/down to zoom in/out.
- While holding left shift, scroll to tilt the layers.

**Note:**

- These camera tricks have not been compared against the actual level editor. They are simply what was easiest to implement.
- For any rendering issues, assume there's something wrong with the tile.
- If you're certain your tile is correct, create an issue report with the tile and the associated init line.
- If said tile is private and should not be leaked, DM me on discord @LudoCrypt.
- I have not tested this on macos or linux as of yet.