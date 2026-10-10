from pathlib import Path
import json, subprocess, math, shutil
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parent
OUT=ROOT.parent if ROOT.name=='source' else ROOT/'Hey-by-Ars-Logo-System'
for d in ['masters','exports','android/app/src/main/res/drawable','android/app/src/main/res/values','android/examples','preview','guidelines','source']:
    (OUT/d).mkdir(parents=True,exist_ok=True)
# Original custom lettering; all coordinates are authored here, no installed font is required.
H='M0 0H60V105H170V0H230V280H170V163H60V280H0Z'
E='M464 191H312C315 222 334 239 362 239C383 239 398 232 410 217L450 249C429 274 401 286 362 286C298 286 256 243 256 178C256 112 297 68 359 68C422 68 464 109 464 175Z M312 150H408C403 124 386 111 360 111C335 111 318 125 312 150Z'
Y='M470 72H533L581 203L631 72H693L609 282C587 339 571 370 522 370C512 370 502 369 491 366V311C501 314 511 315 521 315C540 315 550 303 558 279Z'
def circle(cx,cy,r):
    k=r*0.5522847498
    return f'M{cx+r} {cy}C{cx+r} {cy+k} {cx+k} {cy+r} {cx} {cy+r}C{cx-k} {cy+r} {cx-r} {cy+k} {cx-r} {cy}C{cx-r} {cy-k} {cx-k} {cy-r} {cx} {cy-r}C{cx+k} {cy-r} {cx+r} {cy-k} {cx+r} {cy}Z'
PATHS=[H,E,Y,circle(708,250,30)]
def mark(width=720,small=False):
    s=width/738
    paths=PATHS[:3]+[circle(708,250,34 if small else 30)]
    return paths,s
def lettering(width=720,x=150,y=346,color='#FFFFFF',small=False):
    paths,s=mark(width,small)
    return f'<g fill="{color}" fill-rule="evenodd" transform="translate({x} {y}) scale({s})">'+''.join(f'<path d="{p}"/>' for p in paths)+'</g>'
def svg(content,size=1000):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="0 0 {size} {size}" role="img" aria-label="Hey.">{content}</svg>'
def write(path,txt):
    p=OUT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(txt);return p
def raster(source,target,size):
    subprocess.run(['inkscape',str(source),'--export-type=png',f'--export-filename={target}',f'--export-width={size}',f'--export-height={size}'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
primary=write('masters/hey-primary.svg',svg('<rect width="1000" height="1000" rx="240" fill="#000000"/>'+lettering()))
inverted=write('masters/hey-inverted.svg',svg('<rect width="1000" height="1000" rx="240" fill="#FFFFFF"/>'+lettering(color='#000000')))
small=write('masters/hey-small.svg',svg('<rect width="1000" height="1000" rx="240" fill="#000000"/>'+lettering(small=True)))
mono=write('masters/hey-monochrome.svg',svg(lettering(color='#000000')))
write('masters/hey-wordmark-white.svg',svg(lettering()))
for source,name in [(primary,'hey-primary'),(inverted,'hey-inverted'),(small,'hey-small'),(mono,'hey-monochrome')]:
    for n in ([4096,1024,512,256,128,64,48,32] if name=='hey-primary' else [1024,512,64,32]): raster(source,OUT/f'exports/{name}-{n}.png',n)
for n in [16,24,32,48]: raster(small,OUT/f'exports/hey-favicon-{n}.png',n)
Image.open(OUT/'exports/hey-favicon-48.png').save(OUT/'exports/favicon.ico',sizes=[(16,16),(24,24),(32,32),(48,48)])
# Adaptive mark width 59dp: 81.94% of the usual 72dp masked viewport.
# Optical placement conservatively enclosed in a 66dp-diameter circle.
AD_W=59; AD_X=25; AD_Y=41
adsvg=write('masters/hey-adaptive-foreground.svg',svg(lettering(AD_W,AD_X,AD_Y),108))
write('masters/hey-adaptive-background.svg',svg('<rect width="108" height="108" fill="#000000"/>',108))
write('masters/hey-adaptive-monochrome.svg',svg(lettering(AD_W,AD_X,AD_Y,color='#000000'),108))
raster(adsvg,OUT/'exports/hey-adaptive-foreground-432.png',432)
def vector(width,x,y,color='#FFFFFFFF',size=108,small=False):
    ps,s=mark(width,small)
    return '<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="'+str(size)+'dp" android:height="'+str(size)+'dp" android:viewportWidth="'+str(size)+'" android:viewportHeight="'+str(size)+'">\n'+f'  <group android:translateX="{x}" android:translateY="{y}" android:scaleX="{s:.9f}" android:scaleY="{s:.9f}">\n'+''.join(f'    <path android:fillColor="{color}" android:fillType="evenOdd" android:pathData="{p}"/>\n' for p in ps)+'  </group>\n</vector>\n'
res='android/app/src/main/res/'
write(res+'drawable/ic_launcher_foreground.xml',vector(AD_W,AD_X,AD_Y))
write(res+'drawable/ic_launcher_monochrome.xml',vector(AD_W,AD_X,AD_Y))
write(res+'drawable/ic_launcher_background.xml','<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="#FF000000"/></shape>\n')
for api in [26,33]:
    xml='<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n  <background android:drawable="@drawable/ic_launcher_background"/>\n  <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n'+('  <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n' if api==33 else '')+'</adaptive-icon>\n'
    for name in ['ic_launcher','ic_launcher_round']: write(res+f'mipmap-anydpi-v{api}/{name}.xml',xml)
# Legacy fallback: uses same mask-safe mark as adaptive, to avoid size jumps after upgrades.
legacy=write('masters/hey-legacy-launcher.svg',svg('<rect width="72" height="72" rx="17.28" fill="#000000"/>'+lettering(AD_W,AD_X-18,AD_Y-18),72))
roundlegacy=write('masters/hey-legacy-round.svg',svg('<circle cx="36" cy="36" r="36" fill="#000000"/>'+lettering(AD_W,AD_X-18,AD_Y-18),72))
for density,n in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    for src,name in [(legacy,'ic_launcher'),(roundlegacy,'ic_launcher_round')]:
        p=OUT/(res+f'mipmap-{density}/{name}.png');p.parent.mkdir(parents=True,exist_ok=True);raster(src,p,n)
# Splash without an icon background, white letter paths on an all-black splash window.
write(res+'drawable/ic_splash_logo.xml',vector(170,61,107,size=288))
splash=write('masters/hey-splash.svg',svg(lettering(170,61,107),288))
raster(splash,OUT/'exports/hey-splash-1152.png',1152)
write(res+'values/hey_splash_colors.xml','<resources><color name="hey_splash_black">#FF000000</color></resources>\n')
write('android/examples/AndroidManifest.fragment.xml','<!-- Merge attributes into your existing application; do not replace the manifest. -->\n<application xmlns:android="http://schemas.android.com/apk/res/android" android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round" />\n')
write('android/examples/themes.fragment.xml','<!-- Merge into values/themes.xml after adding AndroidX core-splashscreen. Replace Theme.Hey with the existing post-splash theme. -->\n<resources>\n  <style name="Theme.Hey.Starting" parent="Theme.SplashScreen">\n    <item name="windowSplashScreenBackground">@color/hey_splash_black</item>\n    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_logo</item>\n    <item name="postSplashScreenTheme">@style/Theme.Hey</item>\n  </style>\n</resources>\n')
write('android/examples/MainActivity.fragment.kt','// Add the matching AndroidX dependency and import.\nimport androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen\n\n// In your existing onCreate(), before super.onCreate(savedInstanceState):\ninstallSplashScreen()\n// super.onCreate(savedInstanceState)\n// Keep the existing Activity content.\n')
# Google Play upload: the store applies its own corner mask, full-bleed square supplied.
store=write('masters/hey-play-store.svg',svg('<rect width="1000" height="1000" fill="#000000"/>'+lettering()))
raster(store,OUT/'exports/hey-play-store-512.png',512)
metrics={'version':'1.0.0','canvas':1000,'corner_radius':240,'text_bounds':[150,346,870,346+370*720/738],'text_width':720,'text_scale':720/738,'baseline_y':346+280*720/738,'dot_diameter':60*720/738,'dot_center':[150+708*720/738,346+250*720/738],'adaptive':{'canvas':108,'visible_viewport':72,'text_bounds':[25,41,84,41+370*59/738],'safe_circle_center':[54,54],'safe_circle_diameter':66},'splash':{'canvas':288,'safe_circle_diameter':192,'text_width':170,'offset':[61,107]}}
write('guidelines/logo-spec.json',json.dumps(metrics,indent=2)+'\n')
# Presentation preview, using the actual raster exports, no effects baked into assets.
font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
bold='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def f(n,b=False):return ImageFont.truetype(bold if b else font,n)
board=Image.new('RGB',(1600,1200),'#F5F5F2');d=ImageDraw.Draw(board)
d.text((90,68),'Hey by Ars',font=f(30,True),fill='black');d.text((90,116),'LOGO SYSTEM / 1.0',font=f(16),fill='#666666')
im=Image.open(OUT/'exports/hey-primary-1024.png').resize((620,620),Image.Resampling.LANCZOS);board.paste(im,(90,215),im)
d.text((90,900),'Satu kata. Satu titik. Identitas yang tegas.',font=f(25,True),fill='black')
d.text((90,952),'Custom vector lettering / Pure black & white',font=f(19),fill='#666666')
d.text((850,220),'Primary / inverted',font=f(22,True),fill='black')
for name,x in [('hey-primary',850),('hey-inverted',1120)]:
    icon=Image.open(OUT/f'exports/{name}-512.png').resize((220,220),Image.Resampling.LANCZOS);board.paste(icon,(x,275),icon)
d.text((850,555),'Adaptive launcher masks',font=f(22,True),fill='black')
ad=Image.new('RGBA',(288,288),'black');fg=Image.open(OUT/'exports/hey-adaptive-foreground-432.png').resize((432,432),Image.Resampling.LANCZOS);ad.alpha_composite(fg,(-72,-72))
for i,shape in enumerate(['circle','rounded','squircle']):
    mask=Image.new('L',(288,288));md=ImageDraw.Draw(mask)
    if shape=='circle':md.ellipse((0,0,287,287),fill=255)
    elif shape=='rounded':md.rounded_rectangle((0,0,287,287),radius=69,fill=255)
    else:
        points=[]
        for j in range(720):
            t=j*2*math.pi/720;c=math.cos(t);ss=math.sin(t);points.append((144+143*math.copysign(abs(c)**.5,c),144+143*math.copysign(abs(ss)**.5,ss)))
        md.polygon(points,fill=255)
    icon=ad.copy();icon.putalpha(mask);icon=icon.resize((145,145),Image.Resampling.LANCZOS);board.paste(icon,(850+i*185,620),icon)
d.text((850,825),'Small sizes / 48 · 32 · 24 · 16 px',font=f(22,True),fill='black')
for i,n in enumerate([48,32,24,16]):
    icon=Image.open(OUT/f'exports/hey-favicon-{n}.png');board.paste(icon,(850+i*110,895),icon);d.text((850+i*110,966),str(n),font=f(17),fill='#666666')
d.text((850,1075),'No gradients. No shadows. No extra symbols.',font=f(16),fill='#666666')
board.save(OUT/'preview/Hey-Logo-Overview.png')
print(json.dumps(metrics,indent=2))
