# Getting the Miro board to the agent

You have two options. A screenshot is quicker; a CSV is more reliable.

## Option A: screenshot

1. Zoom so all stickies are readable. Small text gets misread.
2. If the board is wide, take two or three screenshots (left to right) rather than one tiny one.
3. Save them as PNG, for example `board-1.png`, `board-2.png`, in your working folder.

Good for: layout, timeline order, colours. Risk: the agent may misread handwriting-like fonts or small stickies.

## Option B: CSV of the sticky notes

1. Select the stickies (drag a frame around them, or select the frame).
2. Right-click, then **Export** (or the board menu **Export → Save as CSV**; the menu name differs between Miro
   versions).
3. Save the file, for example `board.csv`.

Good for: exact wording. Risk: the CSV has the text but usually not the position or colour. Add the colour by hand
if it's missing (a column `type` with event / command / actor / policy / hot spot), or send a screenshot as well.

## Handing it to the agent

- **Claude Code** reads images and files from a path. Write the path in your prompt:
  *"Here is our board: ./board-1.png ./board-2.png"*. You can also drag the image into the terminal.
- **Copilot Chat** and web chats: attach the image or paste the CSV content.
- The `discover-bounded-contexts` skill also accepts board screenshots. You'll use it in Exercise 5, not today.
- Tell the agent what the colours mean (prompt card 2 does this).

## Privacy

Don't put real customer data on the board or into the agent: no real names, emails, card numbers or addresses.
For your own projects later, check your company's rules before you share any board with an AI tool.
