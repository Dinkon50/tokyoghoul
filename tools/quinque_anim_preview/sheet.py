import sys, glob, os
from PIL import Image, ImageDraw
# usage: sheet.py out.png cols img1 img2 ...  (labels = file stems)
out, cols = sys.argv[1], int(sys.argv[2]); files = sys.argv[3:]
ims = [Image.open(f).convert('RGB') for f in files]
w = max(i.width for i in ims); h = max(i.height for i in ims) + 14
rows = (len(ims) + cols - 1) // cols
sheet = Image.new('RGB', (w * cols, h * rows), (30, 30, 30))
d = ImageDraw.Draw(sheet)
for k, (im, f) in enumerate(zip(ims, files)):
    x, y = (k % cols) * w, (k // cols) * h
    sheet.paste(im, (x, y + 14)); d.text((x + 3, y + 1), os.path.basename(f)[:-4], fill=(255, 255, 255))
sheet.save(out)
