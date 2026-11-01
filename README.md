# Triage

**Download:** [latest release](https://github.com/yungspiderq/triage/releases) · **Bug reports:** [Issues](https://github.com/yungspiderq/triage/issues) · **Build guide:** [BUILDING.md](BUILDING.md)

> Branches: `main` — NeoForge 1.21.1 · `forge-1.20.1` — Forge 1.20.1 · `fabric-1.21.1` — Fabric 1.21.1 · `fabric-1.20.1` — Fabric 1.20.1

> **Seven body parts, each with its own health, instead of one health bar.**
> A hit lands *somewhere*. What it does depends on where.

Minecraft tells you that you have twenty hearts and nothing else. It cannot tell
you that your leg is broken, that your arm has stopped working, or that the cut
you ignored an hour ago is why you are shivering now.

This mod can.

---

## The body

| Part | Health | What losing it costs you |
|:---|:---:|:---|
| **Head** | 10 | dizziness, then darkness — *empty it and you go down* |
| **Thorax** | 22 | everything heals slower — *empty it and you go down* |
| **Stomach** | 16 | food burns away far faster |
| **Arms** | 12 each | slower mining, weaker and slower attacks |
| **Legs** | 14 each | slower, and you can barely jump |

The five parts that are not vital **black out** instead of killing you. They sit
at zero, refuse ordinary healing, and quietly drag your chest down every second
until you deal with them.

Armour, resistance and enchantments all keep working exactly as they did — the
mod only changes where the damage goes *afterwards*. A blow that lands where you
are wearing nothing hurts about a third more, and **nothing covers your arms**,
which is why arms are the limbs that go first.

Press **H** — **J** if Sable: Ragdolls is installed, which wants H for itself — or
click the red cross beside your inventory, for the numbers.
Hovering a limb tells you what is wrong with it, one line and one icon per thing.

---

## What can be wrong with you

| | | |
|:---|:---|:---|
| **Bleeding** | two grades | the light one clots, the heavy one never does |
| **Blood loss** | what bleeding leaves behind | rest and food put it back slowly — a drip puts it back now, once nothing is still open |
| **Cracks** | the warning before a fracture | rest it, or lean on it and find out |
| **Fractures** | never heal on their own | a splint sets it — then it is a crack, and you rest it off |
| | ribs break in numbers | up to four at once, one splint each |
| | a skull or a spine | cannot be splinted at all — those need a surgical kit |
| **Burns** | stop a part healing at all | until salve goes on it |
| **Concussion** | a hard knock to the head | no medicine touches it — while it lasts you cannot read your own condition |
| **Infection** | what an untreated wound becomes | and it has somewhere to go |
| **Collapsed lung** | a broken rib pushed the wrong way | nothing to see, and it gets worse the longer you leave it |
| **Shock** | circulation giving up | a third of your blood gone, or two parts of you gone |
| **Hypothermia** | cold that stopped being a nuisance | your blood will not clot while it lasts |
| **A dead limb** | a strap left on, or rot left in | nothing treats it, and it poisons what it is attached to |
| **A dressing gone through** | a bandage left on too long | fresh, it stops anything under it going bad; stale, it is what keeps the dirt in |
| **Bleeding inside** | a hard blow to a torso | nothing on the figure, no dressing, never clots — only a surgeon ends it |
| **A limb gone** | the only permanent thing here | a wooden leg or a hook gives back about half of one |

### Concussion

The head used to be nothing but the part that kills you fastest. Now a hard blow to
it takes away the one thing this mod otherwise always gives you: knowing exactly how
bad things are.

While it lasts, every number is a **?** and every bar collapses to one of three
lengths — bad, half, mostly fine. Nothing lies to you. You are simply working off an
impression instead of a readout, which is what a rattled head is.

No medicine touches it. It wears off.

### The chain

```
open wound  →  infection  →  fever  →  blood poisoning
```

Blood poisoning does not pass on its own, and it takes your chest apart while you
decide what to do about it.

**That chain is the point.** You can put treatment off. You cannot cancel it.

---

## Going down

Emptying your head or your chest does not kill you outright. It puts you on the
floor with a clock running: you cannot move, cannot fight, and cannot save
yourself. Mobs lose interest — you look dead. Anything that hits you anyway takes
time off the clock.

Somebody else opening you up with a surgical kit can bring you back.

> **Nobody coming is what turns it into a death.**

### Holding them

You do not have to fix somebody to save them. A **bandage or a medkit on a downed
player stops the clock** — it does not heal them and it does not stand them up, it
just holds. That is what the word means: deal with the worst of it now, do the rest
when there is time.

It cannot become immortality. Every further dressing on the same downing holds for
half as long as the last, and **any hit tears it open again**. You also cannot do it
to yourself — from down there, with your own hands, nothing.

So the rescue is two moves now: hold them, then go and get the surgical kit.

On your own, hold **G** and go on your own terms.

### Their airway

A player on the floor is not holding their own airway open. Twenty seconds after they
go down with nobody minding it, air stops getting in — and **the clock runs twice as
fast**.

Opening it costs nothing at all: no item, no cooldown, no minigame. Right-click them
with an empty hand for their chart and the button is along the bottom of it. The count
goes back to zero and starts again, so this is not a thing you fix once — it is a thing
you keep coming back to.

**You can hear it.** Everything else this mod knows about a body belongs to that body
and has to be gone and read on purpose. Somebody who cannot breathe is a noise from the
floor, and anybody near enough to help is near enough to hear it.

### An arrow that stays in

About a third of the arrows that hit you do not come out with the fight. A lodged shaft
is not extra damage — that happened on the way in. It is a wound nobody can deal with
while it is there: **it will not clot, it will not mend, and a bandage over the top of it
is a bandage over an arrow.**

Pulling it costs nothing but a steady hand, and it is **the only thing in this mod that
can leave somebody worse off than before you helped**. Drawn out along the line it went
in on, slowly, it leaves an ordinary cut. Jerked sideways, it tears: a heavy bleed and
damage with it.

So waiting for steadier hands is a real plan. It is also the one free thing you can do to
yourself — the other three are done to somebody who cannot help themselves, and an arrow
in your own arm is not that.

### A bullet that stays in

Arrows are not the only thing that stays. A gun-mod round can lodge too — about one in
four that hits, which is more often than an arrow, because a round that has already gone
through a body has spent most of what it had and stops in the meat.

It is the same wound and not the same way out. A shaft stands out and comes back along its
own track; **a bullet is under the skin**, so there is nothing to take hold of. The wound
has to be opened and the round picked out with forceps. That is the fragment game rather
than the arrow game, and it draws the round as a round — nose, copper band, lying along the
track it came in by — and names the job after it: **Pick out the bullet**. A bad one tears
more, because the jaws are working inside tissue that has already been opened once.

Not every round is the same round. All twenty-five of TaCZ's built-in calibres are kept
apart — `9mm` is a short blunt pistol round, `762x39` a long pointed rifle round, `12g` a
pellet, `rpg_rocket` a finned rocket — with their own length, width, jacket and driving
band, and the operation title and X-ray name what is actually in the limb. A calibre from
another gun pack keeps its own id and borrows the nearest shape.

Until it is out the part will not clot, will not mend and cannot be dressed. The chart
calls it a bullet rather than an arrow, the film shows it, and `[effects.guns]` has the
switches. TaCZ is the one this was built against, and it is NeoForge/Forge only.

### Holding a wound shut

The first thing anybody is taught about bleeding, and the last thing this mod had. A
teammate losing blood and no bandage in your bag used to be a teammate losing blood and
nothing at all.

**A bare hand stops it while the hand is there** — twelve seconds, then it runs again
exactly as it ran. It closes nothing. What it buys is the walk to the dressing, paid for
by standing still. A light bleed goes on clotting underneath, so one held long enough
does close on its own, which is the only way a wound ever closes here without an item.

You do not pick the limb: the hand goes to whatever is running hardest. And it does not
wait for them to fall over — a wound is worth holding shut long before its owner is on
the floor.

### When the clock runs out

It no longer kills. **The heart stops**, and there are twenty-five seconds in which
somebody can still get them back — with the surgical kit, the same as ever. The window
does not save anybody. It buys the time for somebody to arrive.

Your own heartbeat goes silent, and that silence is the message. Everybody nearby hears
one last beat.

**Compressions** are the other free thing your hands can do, from the same chart. They
do not restart the heart, and that is deliberate: compressions that occasionally stood
somebody up would make the surgical kit optional and the arrest a slot machine.
Compressions that only buy time make it a job somebody has to keep doing while somebody
else runs for the kit.

| | |
|:---|:---|
| Every round buys less than the last | 25 seconds, then 15, 9, 5, 3, 1 — about a minute and a half from one pair of hands, and never more |
| Every round is another go at their ribs | thirty per cent a time; they wake up with the evidence |
| Pressing again too soon is refused | not charged for — a round that adds nothing costs nothing |
| A blocked airway drains the window too | at the same doubled rate, so the second pair of hands has somewhere to be |
| A dressing does nothing here | what holds a clock cannot start a heart |

None of the four is a button. Compressions are a rhythm — a marker sweeps, the beat is
the middle of it, a hundred and ten a minute. Pressure is a force that climbs while you
hold and falls while you do not. The airway is an angle that sags back out of true the
moment nobody is holding it. The arrow is the line it came in on, and the speed you dare
take it at. None of them can be failed except the arrow; doing one badly is worth less,
and never less than a third.

**Your hands go on a limb, not on a button.** Click the part — on the figure or in the
list — and the hands go there. One thing to do happens; several open a short list under
the cursor. The airway is the head of somebody on the floor, and compressions are the
chest of somebody whose heart has stopped.

### What do I do about this

The chart says what is wrong. The **?** beside its title says what to do about it —
nineteen pages of what a thing is, what it does to you, and what answers it.

The answers are not written down in there. Each page builds the injury it describes and
asks the mod itself what treats it, which is the same question the treatment screen asks
when it greys out a limb. A guide written by hand would be recommending splints for
skulls two versions from now; this one cannot, because it does not know anything — it
only knows how to ask.

### Carrying them out

Surgery where they fell is often no surgery at all — the fire is still burning
and the thing that put them there is still standing over them.

**Shift + right-click with an empty hand** and you pick them up. Your hands are
full: you cannot swing at anything, and you move at half speed.

**Right-click anything** — or press **G** — and they are laid out on the ground in
front of you.

The clock keeps running while you carry, at half pace. Somebody has hold of them,
and that counts for something — just not for enough.

### Going down for real

Triage's own version of lying down is a forced swimming pose. It is the same shape
whatever put you there, it clips nothing on the way, and next to a body that
actually fell it reads as an animation.

Install **[Sable: Ragdolls](https://modrinth.com/project/I3mWDgfy)** or
**[Ragdollified](https://modrinth.com/mod/ragdollified)** and it stops being a pose. A
downed player is dropped by the physics engine at whatever speed they were carrying, and
lies where they land — **and cannot climb off the doll**, which is this mod's oldest rule
written in somebody else's.

The doll runs on Triage's clock rather than its own, and is let go the moment the
downing ends: surgery, giving up, death, a respawn. Picking somebody up lets go of
it first — hands beat physics — and setting them down drops them again.

A falling ragdoll used to arrive unhurt — a passenger takes no fall damage, and the
physics mod deals none. Triage measures the drop and deals it now, and it lands on a
part rather than on your feet: land head first and you get a concussion, land chest
first and you get ribs.

> Nothing is required. Without either mod the pose is the pose, and none of this code
> is even loaded. With **Sable** installed, Triage's **look yourself over** key moves
> off **H** to **J**, because that is the key Ragdolls binds. Ragdollified does not take
> H, so the key stays where it was. Turn the whole thing off with `effects.ragdoll`.

---

## What is inside a torso

Three of the seven parts have something in them, and a hard enough blow tears it. From that
moment the blood is going somewhere nobody can see — into the belly, into the chest, anywhere
but out. Nothing appears on the figure, no dressing touches it, and it never clots, because
there is nothing on the outside for a clot to close.

It is on the chart, so it can be found by reading. It cannot be found by looking, and that is
the whole of it — the injury that kills somebody who was walking and talking a minute ago.
One thing ends it, and it is a surgeon. A bruise inside settles on its own; a tear does not.

**There are eight of them, and each is a different emergency.** A heart empties somebody
faster than anything else in the mod and does not give them three minutes; tear it and
there is a better-than-one-in-three chance the heart stops outright, which is an arrest
and not merely a bleed. A spleen is a bag of blood a kick can burst. A liver is big and
soft and sits right under the ribs — and it is under the *right* ones, where the spleen is
under the left, so a kick to one side is not the same emergency as a kick to the other. A
lung crushes itself on top of bleeding, which the needle answers. A gut barely bleeds at
all and is the one that frightens people, because what comes out of it is not blood and
some minutes later it is sepsis. A brain is the only thing in here a surgeon cannot put
back.

**The figure turns over.** A button in the top right corner of the figure shows you the same
person from the inside — same place, same size, so switching is turning a body over rather
than going somewhere and coming back. Every organ has its own shape where it actually sits:
the heart between the lungs and left of the middle, the liver a wedge under the ribs tapering
across the midline, the spleen a small oval on the other side, the kidneys flanking, the gut
filling the bottom, with faint ribs behind all of it. A tear beats; a bruise is amber and
still. Hovering gives the organ, its bleed multiplier, and what that one is frightening for.
The button's own square goes red or amber first, so you never look on spec.

**And you can feel for a pulse.** Two fingers on a wrist — free, no item, no light, and it
works on somebody still standing up. It is the first thing anybody does to anybody, and here
it is the only outward sign of the injury that has none: a body losing blood drives the heart
faster, so a fast thin pulse on somebody with nothing open on them is the tell. The mod says
where the blood went; the pulse says that it went.

Blood pushes it up hardest. Shock, being down and pain push it up further. Painkillers and
anaesthesia hold it down, and hypothermia holds it down hard — which makes a slow pulse on
somebody who ought to be racing worse news, not better. A stopped heart is nothing under your
fingers.

The game is the measurement: a place on the wrist and beats arriving at the rate that heart is
really running at, and the job is to hold still while enough of them go past. Do it properly
and you hear the tachycardia before anybody tells you about it. Hold steady and you come away
with the number; let your hand slide and you come away with "fast", which is most of what the
number was for. Nothing you do can make anybody worse.

**An explosion is four injuries at once.** The wave comes first and it is the one that
matters: pressure does nothing to muscle, tears anything with gas in it — lungs first, then
bowel — and leaves no mark at all. Somebody walks away from a blast talking and drowns in
their own chest later. Then the fragments, several small wounds at once, which are the same
problem an arrow is and come out the same way. Then the throw, then the heat.

And the rule that makes it worth having: **armour stops fragments and does nothing for the
wave.** Plate is a very good answer to a piece of metal travelling at somebody and no answer
at all to the air around it. So the hardest casualty in the mod is the one in full armour who
walked out of the centre without a scratch and two torn lungs — nothing to examine, and the
only way to find them is the chest.

**You can see where somebody is bleeding.** It is on the model, on the part it is coming out
of — painted rather than built out of blocks, so it has a ragged edge, runs downhill and thins
as it goes. Light and heavy differ by how much of it there is rather than by shade. Nothing is
drawn under a dressing, and a clean dressing does not drip either.

**Somebody bleeding leaves it on the ground.** The marks lie flat where they fell and dry
from red to brown, so a trail says which way somebody went — follow the fresh end. Walking
leaves a line of drops; standing still leaves one pool that keeps growing, and where the pool
is, is where they stopped being able to walk.

**And a blast deafens.** The injury everybody near an explosion has and nobody counts. It
wears off, and while it lasts it takes the one sense the mod uses to warn you about yourself:
no heartbeat in your ears, a pulse that has to be counted by eye, and — the point — a deafened
patient cannot hear the question. The only channel that says *where* stops working on exactly
the casualty whose worst injury leaves nothing to see.

**You can watch a chest.** The one vital you get without touching anybody, and the first in
the order everybody is taught. It does for a crushed lung what the pulse does for internal
bleeding: a pneumothorax has never had an outward sign, and fast and shallow is what one
looks like. Slow outranks shallow, because nine breaths a minute is a chest about to stop —
and that is the only vital here spoiled by the person treating rather than by the injury.
Ether and painkillers both press on the drive to breathe.

**And feel a forehead.** The slowest vital and the only one that sees the future: it climbs
while an infection is still a colour on the chart and long before it is a fever with a name,
so somebody who checks catches the thing that kills quietly. It runs the other way too — lost
blood and cold blood both pull it down, and cold blood does not clot, which is the loop that
kills a casualty who was going to live.

**And you can ask them.** Every other reading here is taken off a body; this one comes out of
one. It is the only thing that says *where* — a pulse says the blood went somewhere, and a
conscious person says which side of their belly it went into. It stops working when they stop
being able to talk, which is itself worth knowing. A rattled head names something that hurts
rather than the worst thing, and you are told when the answer came from one. And painkillers
work: a dosed patient reports less than is true — small complaints go entirely, big ones come
down a grade — so you keep the location and lose the urgency, which is what decides who gets
treated first. Nothing else in the mod makes a treatment cost information.

All three sit on one row of the chart with the pulse, under the volume, because they are read
together.

**And a ribcage is a shield.** A blow to a torso where the bone is already broken tears
something inside twice as often; a crack counts for half. That turns a broken rib from a
thing that hurts into a thing that has stopped protecting you, and gives the splint a second
reason to exist that nobody has to be told about: the second blow is the dangerous one.

## Taking it off again

A dressing comes off by hand for nothing, and a fresh one lifts clean. One that has been on
two minutes has dried onto the wound — what is holding it there is the thing that was closing
underneath — and fingers take that with them, so it opens again. Bandage scissors do not care:
the blade against the skin is the blunt one, so the cloth comes away in one piece and the
wound never knows. They are not spent, and that is the whole reason to carry a pair.

A strap comes off by hand too, and nothing goes wrong with it — what happens is simply
whatever it was holding back, because the wound under a tourniquet was never closed.

**A strap costs the limb it is on, and only that limb.** The damage it used to do overflowed
into the chest the way a blow does, so a tourniquet round a forearm was hitting a ribcage and
the limb emptied long before its three-minute clock could matter. It goes onto the strapped
part now and nowhere else, at a rate the clock outlives.

**And letting a long one go costs the rest of you.** A limb with no blood moving through it
does not simply wait, and what has built up in it goes everywhere the moment the strap comes
off — nothing for the first third of the clock, and worse the longer you leave it. Which is
the argument a tourniquet actually makes: at some point the question stops being when to
release it and becomes whether to release it or cut below it.

It is visible on the body too, and it darkens as the deadline comes up — so you can see from
across the room that somebody's is due.

## Being awake for it

Two things here are done to a patient who can feel all of it: a surgical kit opening them up
and a saw taking their leg off. Without anaesthetic both still work, and both say what that
cost — the quality is capped however good the hands were, the part pays three on top, and
the pain alone is an even chance of shock.

Nobody can put themselves under and then operate, so this is the one item whose whole value
is what it lets somebody else do properly. Under, a patient cannot move or swing, the lights
are off, and they can be carried; it wears off on a clock and they get up.

The dose is the danger. The mask has no gauge on it — you read the patient, a drop at a time,
and too much stops their breathing. Which this mod already has an answer to.

## Losing one

Everything above can be walked back given enough bandages and enough patience. Two things
cannot, and both of them start with a decision somebody made on purpose.

**A tourniquet has three minutes in it.** It stops the bleeding dead and starts a clock on
whatever is under it. You are told out loud with a minute to go, and the count runs back
down the moment the strap comes off - so it is a deadline rather than a punishment, and
the only way to reach the end of it is to keep not dealing with it. A severe infection
nobody cleaned takes four minutes to the same place.

**Past that the limb is dead.** No dressing, no medkit and no surgeon touches it. It has
no health and it never will, everything that was wrong with it stops being a wound, and it
poisons the chest it is attached to until somebody takes it off - a fifth again what a
blacked-out limb costs, plus a standing chance of the blood going bad.

**The saw takes it off.** Only ever a limb that has already died, so it is never a clever
shortcut. Put a tourniquet above the cut first: with one there is a stump that clots like
any other wound, without one there is an artery open on it that never will and a fifth of
your volume on the floor. It costs blood either way and it puts the patient into shock
either way. What it buys is that the chest stops paying for a limb that is not coming back.

The game is strokes - full ones, end to end, level, eleven of them. It cannot be failed.
There is no version of putting the saw down halfway that leaves anybody better off, so
there is no way to put it down; doing it badly costs the patient blood instead. The saw
itself is not spent. It is a tool.

**With both legs gone you do not stand.** Not the downed state - nobody is dying and
there is no clock. There is simply nothing to stand on. With Sable: Ragdolls installed you
drop into a ragdoll; without it you get the same pose a downed player gets, and you crawl.
Your hands still work, so a legless surgeon is still a surgeon. Somebody can pick you up
and carry you. **One wooden leg gets you back on your feet**, which is the whole argument
for having built them. Legs blacked out but still attached hold you up as before: a
surgeon brings one of those back in ten seconds, and nothing brings back one that is gone.

**Then a wooden leg or a hook**, fitted to a stump that has stopped bleeding. Neither is
the limb. They give back about half of one, which is the difference between walking and
dragging, and they are the only thing at the end of the worst road in the mod.

**Or better ones.** A jointed leg and a mechanical hand, and the entire difference is one
part: a joint. A peg is a stick that meets the ground and everything above it swings round
it; a jointed one bends where a leg bends. That is three quarters of a limb instead of under
half. They go onto a bare stump or over a plain one, they cost leather, iron and redstone,
and the medic sells them at master.

**And they wear out.** Nothing made of wood and leather and worn all day stays the shape it
was cut to: a socket loosens, a strap stretches, a joint develops play. A worn prosthesis is
not a broken one — it never falls off and never demands anything urgent — it is simply less
of a leg, sliding to a bit over half of what it was fitted at. You get one message about it,
at the point where it wants seeing to. The fix is another prosthesis on the same stump, and
a fresh one over a fresh one is refused rather than quietly spent. Losing a leg is not an
event somebody gets past; it is a thing they then live with, and living with one is mostly
maintenance.

**It shows.** The part is not drawn - on you, on everybody watching, in the first person,
and under your armour. Nothing is held in a hand that is not there. What was fitted in its
place is there to see, swinging off the joint the limb swung off, and you can tell which of
the four it is: a peg, a hook, a thigh and shin with a brass knee between them, or a hand
with fingers. Different silhouettes, not different paint. It is also the second
thing about somebody else's body that needs no walking over to find out, after the triage
mark: a person's shape is visible from across a field.

**And it still hurts.** Nearly everybody who loses one reports the limb that is not there.
It costs nothing and does nothing. Painkillers work on it, which is the only thing anybody
has ever been able to offer for it.

---

## Two difficulties

**Arcade** is the mod as it has always been, and it is the default: the body chart is a
readout. Open it and it says the left leg has two breaks and an infection.

**Realistic** changes exactly one thing — where that knowledge comes from. Same injuries,
same rates, same treatments, same minigames. What goes is the free answer: **a part nobody
has examined reads as a blank**, and the difference between "nothing there" and "did not
look" is the whole of the mode.

Everything needed to fill the chart in was already here, built one version at a time — a
pulse, asking where it hurts, asking for a number out of ten, pressing on a burn, a
thermometer, an X-ray — and two new ones:

| | What it finds | What it costs |
|:---|:---|:---|
| **Feeling along a bone** | a break that has not moved out of line — the one thing nothing else can find without a machine | pressure on something already broken, and a clumsy press moves it |
| **A stethoscope** | air moving on both sides of a chest, or on one | four seconds of quiet, which a battlefield does not sell cheaply |

What is plain stays plain in both: blood, a missing limb, a burn on the skin, a bone through
it, a leg lying at an angle it does not bend at. The mode hides what is inside somebody, and
never what anybody in the room can see.

The switch is `[difficulty] realistic` in the server config, so it travels with the world —
one world, one difficulty, for everybody in it.

## The medicine

| Item | Crafted from | What it does |
|:---|:---|:---|
| **Bandage** ×2 | wool + string | stops bleeding, patches the part up |
| **Tourniquet** ×2 | leather + string + stick | stops bleeding *now* — and starts killing the limb |
| **Splint** | 2 sticks + string | sets a broken or cracked bone |
| **Medkit** | 2 bandages + glistering melon slice + leather | dresses a wound |
| **Surgical kit** | medkit + ghast tear + 2 iron ingots | brings back a blacked-out limb, and people off the floor |
| **Antiseptic** ×2 | glass bottle + 2 sugar cane + brown mushroom | cleans an infected wound out |
| **Burn salve** ×2 | honey bottle + 2 slime balls | takes a burn off a part |
| **Painkillers** ×2 | sugar + glowstone dust + brown mushroom | hides the penalties. Heals nothing, and each course in a row is worth less |
| **Antibiotics** | glass bottle + 2 brown mushrooms + nether wart | cures one illness |
| **Antipsychotic** ×2 | glass bottle + nether wart + ghast tear + sugar | holds one illness down while it lasts. Cures nothing |
| **Anaesthetic** | glass bottle + fermented spider eye + sugar + bandage | puts somebody under. Nothing else here can kill by working too well |
| **Adrenaline syringe** ×2 | bottle + blaze powder + sugar + iron nugget | a stopped heart, or a reaction. Never into a bleed |
| **Morphine syringe** ×2 | bottle + 2 poppies + iron nugget | instant and strong. A bad chest may stop breathing |
| **Naloxone syringe** ×2 | bottle + fermented spider eye + sugar + iron nugget | undoes morphine, all of it, at once |
| **Bandage scissors** | 2 iron ingots | cuts a dressing off without touching the wound. Never spent |
| **Jointed leg** | leather + 2 iron + redstone | a leg with a knee. 0.75 of one instead of 0.45 |
| **Mechanical hand** | leather + iron + redstone | a hand that opens, rather than a hook that does not |
| **Saline drip** | antiseptic + glass bottle + leather + string | puts volume back — refused while anything is still open |
| **Blood bag** ×2 | leather + 2 string + iron nugget | empty: draws blood. Full: gives it |
| **Medical table** | 2 bandages + 2 planks | a workstation. Put one down and a villager becomes a medic |
| **Triage tag** ×4 | 2 paper + string | blank. Somebody writes one colour of four on it |
| **Decompression needle** ×2 | iron ingot + glass pane + antiseptic | lets the air out of a crushed chest |
| **Bone saw** | 3 iron ingots + stick | takes a dead limb off. Never spent — it is a tool |
| **Wooden leg** | leather + 2 planks | fitted to a leg that is gone. Gives back about half of one |
| **Hook** | leather + 2 iron ingots | the same for an arm, and honest about being a hook |

### A dressing is a thing on somebody

A bandage used to be an event: it went on, the bleeding stopped, and that was the end of the
thought. It is now a thing you are wearing, and it has two halves.

While it is fresh it is the best item in the box — **nothing under a clean dressing can go
bad at all**, which is the only thing in the mod that prevents something rather than dealing
with it afterwards. Left on for eight minutes it becomes what is keeping the dirt in, and
from that moment a wound can turn with no waiting at all. That is the commonest way a treated
wound becomes an infected one, and it now happens here for the same reason.

The answer is another bandage — the dirty one comes off with it. Antiseptic takes it off too,
because cleaning a wound means opening it. And it comes off by itself once there is nothing
underneath worth dressing, so nobody is changing the bandage on an arm that healed twenty
minutes ago.

You can see it, too: one band per part, cream while it is fresh and the colour of a dressing
nobody has changed once it is not. Like a triage mark and a missing limb, it costs nobody a
walk over to find out.

### None of it works by simply clicking

Using an item opens a screen where you pick the part yourself — and then do the
work by hand.

- **Bandage** — wind it round the limb in circles, three times round. Go back the
  way you came and it comes off again.
- **Tourniquet** — two hard turns of strap. Take it off with a bandage when you
  have a moment.
- **Splint** — pull the bone back into line and hold it there while it slips.
- **Medkit** — four points along the wound, each before its ring closes.
- **Antiseptic** — scrub it out, edge to edge.
- **Burn salve** — cover the burn without wasting any. There is barely enough.
- **Antibiotics** — stop the needle on the mark. A cold takes one dose; blood
  poisoning takes three.
- **Saline drip** — find the vein and stay in it while the bag empties. The arm
  does not hold still, and neither does the vein.
- **Blood bag** — the same needle, in whichever direction the blood is going.
- **Surgical kit** — a procedure, not a trick: clean the field, draw the fragment
  up the channel without touching the sides, then stitch it shut.

Three slips end the attempt. So does being set upon while you work — **this
screen does not pause the game** — though a scratch will not. Nothing is ever
lost but time.

---

## The medic

Crafting was the only source of medicine, which meant a player who never went looking
for nether wart simply had no antibiotics — and no way to know that was the problem.

**Villages now generate a clinic**, in whatever the local village is built out of. Two
beds with an aisle between them, a bench along the south wall in the order anybody would
use it - water, the table, a brewing stand, a barrel of supplies - a stone floor under
that bench and planks everywhere else, and a gabled roof, or a flat one with a parapet
where the village is a desert one.

The villager who takes the job at that table sells what the mod expects you to carry, and
buys the raw materials for it.

| Level | Sells |
|:---|:---|
| Novice | bandages, triage tags — and buys your string |
| Apprentice | splints, tourniquets — and buys your leather |
| Journeyman | antiseptic, painkillers — and buys your sugar cane |
| Expert | medkits, burn salve, antibiotics |
| Master | saline, blood bags, surgical kits |

The two that actually decide a fight are the last things they will sell you. Put down
your own medical table and any unemployed villager will take the job.

---

## Blood

Everybody has a blood type, and it is **rolled again every time you respawn**. Yours
is on the body screen; a patient's is in the header while you are working on them; a
bag's is on the bag.

Blood may only go into somebody who already carries every antigen it does — the real
ABO and Rh rules. That makes **O−** the thing worth hoarding and **AB+** a lucky
thing to be.

| | |
|:---|:---|
| **Drawing** | an empty bag on somebody takes a third of their blood. They feel it |
| **Giving** | a matching bag puts back more than saline ever could |
| **Getting it wrong** | the bag is spent, and they are worse off than if you had done nothing |

The wrong blood is the one thing in this mod that punishes you for treating somebody
rather than for failing to. It damages the chest, it drains more blood than the
transfusion put in, and no course of anything shortens it. **Read the bag.**

> A stockpile of matched blood stops matching the moment its owner dies. Blood is
> worth what it is worth to the group, not to the person carrying it.

---

## Looking before treating

**Right-click a player with an empty hand** and you get their chart: every part, every
condition, blood left, blood type, whatever illness is running. Nothing is spent and
nothing is started — it is a look, not a treatment. Shift is still "pick them up".

It is a snapshot. If you want to know whether they have got worse, look again.

The chart also says how long somebody on the floor has left, and whether a dressing is
holding the clock — the one number that used to be visible only to the person it was
counting down.

> A concussion applies to whoever is reading, not to whoever is read. A rattled medic
> makes a poor job of somebody else's chart too.

---

## Sorting them

A look is worth nothing to anybody but the person who took it. **Triage tags** are
where the answer goes: four buttons at the top of somebody's chart, one blank tag out
of your pockets, and from then on everybody can read it.

| | | |
|:---|:---|:---|
| `[!]` | **Immediate** | now, or not at all |
| `[~]` | **Delayed** | hurt, and will still be hurt in five minutes |
| `[-]` | **Walking** | on their feet and able to carry their own bandage |
| `[x]` | **Expectant** | nothing anybody here is carrying would change this |

The mark hangs off their **nameplate**, so the sorting is done at the distance sorting
actually happens at. A player who is on the floor has their name in dark red, whether
or not anybody has marked them.

The mod never decides which colour is right. Deciding that is the job.

> **A mark is a snapshot too.** It never updates itself. After two minutes it keeps its
> colour and picks up a question mark, and the mod stops claiming to know how old it is.
> A yellow tag on somebody who went down a minute ago is not a bug — it is the reason
> anybody walks back and looks again.

Only the person wearing a tag can tear it off. Writing a new one over it costs another
tag. Tags are paper and string, four to a craft, and the village medic sells them by
the handful.

---

## Treating somebody else

Right-click another player with medicine and it is **their** body on the screen:
their limbs, their bleeding, their broken bones. The supplies come out of your
pockets, and both of you are told what happened.

Stay within five blocks and stay on your feet.

That is what the surgical kit is really for — somebody has to hold still while
somebody else does the digging.

---

## Where you operate, and with what

A treatment used to be worth the same wherever it happened and whatever it was done
with. Both halves of that were wrong.

**The room.** A medical table within four blocks, light to work by, and a roof — worth
0.5, 0.3 and 0.2 between them. The table is worth most because it is the one you have
to decide to build; the other two are nearly free indoors.

| Place | Quality | Infection with a clean tool |
|:---|:---|:---|
| Open sky, in the dark | ×0.75 | 18% |
| A cave: roof yes, light no | ×0.85 | 11% |
| **An ordinary lit room** | **×1.00** | **0%** |
| That room with a table | ×1.25 | 0% |

An ordinary lit room is the middle, and the middle is exactly what a treatment has
always been worth. Nothing about the way you already play costs you anything new.

**The instrument.** The saw and the forceps go into a wound and are never spent, so
they are never clean either until somebody cleans them. Six patients and it is as dirty
as it gets: 55% that the treatment leaves something behind. Right-click a medical table
with it to scrub it — free, and it does not travel. Antiseptic does the whole bag in the
field and costs the bottle.

Bandage scissors are left out of it. Nothing here has them opening a wound, so dirt on
them would be a chore with nothing at the end of it.

How dirty it is shows on the item itself — "Clean", the count since it was last cleaned,
and "Filthy" in red once there is nowhere worse to go.

---

## A second pair of hands

The third thing beside where and with what: **who**.

Crouch next to the patient with an empty hand and you are holding them. Both are
deliberate — nobody does either by walking past. One person counts, not a crowd.

- **+0.15 on how well the work goes**, wherever you are standing.
- **The ceiling on operating awake rises from 0.5 to 0.7.** Held down, a patient with no
  anaesthetic moves less. That is what an operation was before anaesthetic, and why every
  picture of an old operating table has straps on it. Still worse than the bottle.

Both people are told when it counted, because an assistant crouching with an empty hand
looks exactly like an assistant crouching.

---

## The three needles

Everything else is something you do with your hands over a minute. These are decisions made
in a second, and each can be the wrong one.

**Adrenaline** gives a stopped heart back the value of its next round of compressions, and
answers a reaction to blood. Its wrong use looks identical to its right one: somebody
emptying out looks like somebody fading, and adrenaline raises the pressure — every light
bleed turns heavy. A second shot on top of the first is the other way a heart stops.

**Morphine** is instant, strong, works on somebody on the floor, and ignores the pill
tolerance. What it costs is breathing: on a chest already failing it can stop it altogether.

**Naloxone** undoes morphine and nothing else — the difference between a patient and a body
when breathing has gone, and a cruelty on somebody the dose was holding together.

**The game is the vein.** Blood in the hub is the only confirmation there is: inject with it
and the dose is in the circulation, inject without it and it is in the muscle. Drag to aim,
drag right to advance, slowly — a stab goes through. Drawing back is free. And the
difficulty is the patient: empty, shocked or cold veins are flat and barely visible, so the
needle is hardest to place in exactly the person who needs it.

---

## Where it hurts is not where it is

A ruptured spleen hurts at **the tip of the left shoulder** — Kehr's sign, and it is how
that injury gets found with no machine to hand. A torn liver does it on the right.

Ask a patient where it hurts and they may send you to the wrong place. They are not lying
and not confused; they feel it there. Nothing marks the answer as suspect.

What gives it away is everything else: the chart says that shoulder is untouched, the
complaint says it is the worst thing in the body, and the pulse is running with nothing
open on them. The belly is filling up.

It only ever fires when the named place is **genuinely unhurt**, so the contradiction is
always real — if the shoulder were broken too, they would have been complaining about it
anyway. The only tell in the words is that they say *the top of* the arm.

---

## Cold, and why it kills

Three things kill a badly hurt person together, and each makes the next two worse. Two of
them were already here.

**Cold blood does not clot** — hypothermia stops a bleed closing by itself at all. And
**losing blood is cooling**, which is why the temperature falls with the volume.

The third closes the circle: **an open body loses heat.** Opening somebody is what cools
them — which is why a theatre is kept uncomfortably warm for everybody standing up in it.

A lit room out of the weather costs almost nothing. The open, in the snow, in the rain, at
night, costs a great deal — and enough of it is hypothermia, and then nothing closes on its
own any more. It reads on the forehead before it gets there, and both people are told.

There is no item for it. Somewhere warm, a fire, a roof, and be quick.

---

## Colds, and who gave them to you

A sneeze is loud, catching, and it hurts.

- Anybody **within four blocks** can pick the cold up off you — the first thing here that
  travels from one player to another. A cold and nothing worse, on purpose.
- Sneezing with a **broken rib** costs you. If your chest or belly is still knitting from
  an operation, a sneeze can **open it** — the only way here to come apart doing nothing.
- A cold left in a body that stays **cold and wet** goes down into the chest after about
  three minutes. Getting dry and warm is the whole of the answer.

---

## Illness with a place

Two illnesses happen to an organ rather than to a person.

**Pneumonia** bruises both lungs rather than tearing anything — there is no hole for a
surgeon to find, so opening the chest achieves nothing. It reads on the breathing line and
on the forehead, and only a course answers it.

**Peritonitis** is a belly full of what was meant to stay in the gut. A torn gut used to
roll straight for sepsis; this is the step in between. It is also the one illness
antibiotics cannot finish — while the gut is open the cause is still arriving, so a course
buys time and the surgeon ends it.

---

## Not feeling it yet

A wound used to cost you from the tick it happened. A broken bone, or a limb going out,
now buys about **25 seconds** in which most of the pain is not there — people finish races
on stress fractures and walk away from crashes on broken ankles.

**It hides pain. It does not mend bone.** A part that is gone, dead, or beaten past working
at all gets nothing from it: what is missing there is not the will to ignore it.

- **+30 on the pulse** — the only sign of it anybody else can read.
- **Your hands shake**: −0.15 on anything you treat during it.
- **Then it arrives at once**, 20 seconds a third worse than baseline.

No way to ask for it, and no second one until the first is paid for.

---

## Arms give out

Carrying somebody and bracing somebody are the two physically heavy things here, and this
mod invented both of them. A medic used to be able to do either forever.

**It is not a stamina bar.** Sprinting does not build it, mining does not, fighting does
not — vanilla already charges for those.

- **Carrying drains it** — ninety seconds with sound arms. **Bracing a patient costs a
  lump**, paid when the treatment lands.
- A carried body **gets heavier as you tire**, and at the end your arms give out and set
  them down wherever you are. Not damage; the wrong place.
- **A broken, dead or missing arm tires up to three times faster.**
- **Tired arms are not steady ones**: up to −0.25 on how a treatment goes.

Back over 45 seconds, twice as fast standing still. `/triage info` says nothing at all
while your arms are fresh.

---

## After the operation

An operated part is **knitting**, and nothing hurries it. There is no item for this.

It is not a debuff — nothing is taken away while it lasts. A mending arm swings exactly
as well as it did an hour ago. It is a wound that has not finished being one: sprint on
a mending leg, swing or mine with a mending arm, and it **comes open again**. Heavy
bleeding, the clock back to zero, and somebody has to close it a second time.

The trunk is worked by both. The head is never worked. Lying low is not a faster clock,
it is the absence of the thing that resets it — which is why this is not a debuff.

45 seconds for a job done well in a proper theatre, up to 135 for a bad one on a floor.

---

## A temperature needs an instrument

Every other reading here is taken with the hands and is there the moment you look. A
temperature is not. Anybody can feel that a forehead is hot; **thirty-eight point six** is
not something any hand has ever reported.

So the word is free — the chart still says hot, cold, normal — and **the figure costs a
thermometer**, plus the half minute the column takes to settle. The column climbs fast,
then slower, then hardly at all, and the whole skill is knowing it has stopped. Pull it
early and the number is too low, and nothing about it looks wrong.

It cannot be failed. A thermometer taken out early hands over a band rather than a figure.

Two glass panes, redstone and an iron nugget. It is not consumed.

---

## Two illnesses a bottle cannot touch

**A course of antibiotics does nothing to a virus.** Not less — nothing, except that the
course is gone. Everything the bottle answered was bacterial, and there was no need to say
so until there was something else.

**Influenza** runs hot and spreads off a sneeze the way a cold does. It comes from where
influenza comes from, which is a crowd — stand about in a village long enough and somebody
in it was ill. It passes on its own given time and somewhere warm, and most of the time
that is all it does. What it can do on the way is leave the chest open to **pneumonia**,
which is how influenza has always killed people and the one part of it a bottle does
answer. Whether that happens is decided by the body: cold, wet or three days awake and the
chest is likely to open; dry, warm and rested and it will not open at all.

**The stomach one** comes off what was eaten. It does not burn, it empties — it pulls
volume, it has no cure, and what it needs is fluid faster than it is going.

And the forehead is how you tell them apart: a virus and an infected wound both run hot,
and only one of them is worth a bottle.

---

## Not sleeping

Minecraft has always counted this and has always done one thing with it, which is to send
birds. What being awake for three days actually does is from the inside: **a tired pair of
hands is a worse pair of hands, and their owner is the last to know it.**

- −0.2 on anything you treat — the fifth thing that decides an operation, and the only one
  that cannot be fixed in the next thirty seconds.
- Twice as easy to catch what is going around.
- Your own wounds take half again as long to knit.

The first day is free. Nothing is stored for any of it: the counter is vanilla's own, the
one the phantoms read.

---

## A burn has a depth

Three kinds, and which one you have decides everything else.

| | |
|:---|:---|
| **Superficial** | red, painful, closes itself once it stops cooking. Nothing has to be done |
| **Partial thickness** | blistered, the most painful of the three. This is what a salve is for |
| **Full thickness** | through the skin. Does not hurt, will not close, a salve is no use |

**The worst one does not hurt.** The nerve endings that would have done the hurting are
part of what burnt — so the patch somebody is screaming about is the one that will be fine,
and the quiet patch beside it is the one that needs a knife. It costs nothing in pain and
says nothing when you ask them where it hurts, which is why a burn is **pressed on** rather
than looked at.

**It is not over when the fire is out.** A burn goes on deciding how deep it is for about
half a minute. Water in that window holds it where it is and can take it back a step; snow
and ice do the opposite.

**What kills is the size.** Add up what is burnt past the skin — head nine, each arm nine,
each leg eighteen, chest and belly eighteen apiece — and past about fifteen percent of
somebody, fluid goes out through their own surface faster than resting can replace it.
Lying down does not answer that. A bag of saline does.

### Pressing on one

Free, works on somebody standing up, and the only reading here whose finding is an
absence. Press across the burn: where it hurts there is something living under it, and
where pressing does nothing at all the burn is through the skin. Press in one place and you
learn it is burnt, which you could see. Press across the whole of it and the mod names the
depth and tells you which answer it wants — the jar, or the knife.

### Breathing it

Everything on the outside of somebody is measured in hours. **An airway is measured in
minutes**, and it happens to people who walked out looking fine. Caught in fire or lava,
with nothing on the skin to show it: the tell is the voice going and the breathing turning
fast and shallow.

No bottle touches it — this is swelling, not infection. Two things make it lethal, and both
were already here: **morphine**, which presses on the drive to breathe, and **a face burnt
through**, which together with the smoke is what closes an airway. What answers a closed
airway is a pair of hands, over and over, for as long as it takes.

---

## Things that are not there

Some bodies can arrive with **schizophrenia**. It is rolled with the blood type, at the
same moment and for the same reason: it is a fact about a person rather than an event in
their day. It is **on by default**, as it has been since it was added, and ten in a hundred
bodies have it. A group that does not want it turns it off.

It is not written the way games write it. Real voices comment, criticise and repeat
themselves — they do not hand out orders, and the version that does is the most repeated
untruth about the illness. There is no "kill them" in here.

| what happens | |
|:---|:---|
| **Voices** | a noise from where nothing is, a line of chat with no name on it, a word across the screen — or a message with another player's name on it that they never wrote |
| **Figures** | somebody standing about nearby, wearing the face of a player really on the server. It walks, works, crouches, follows at a distance it never closes, sometimes turns and watches |
| **Mislabelling** | two limbs swap names on the chart. The bars and numbers are right; the word is wrong |
| **The quiet half** | not wanting to do anything, and a body looked after worse for it: blood comes back half as fast |

All of it happens on one client. Nothing is sent to the server, no entity exists in the
world, and nobody else sees or hears any of it. The figures give themselves away by not
surviving contact: swing and your hand goes through nothing, walk up and there is nobody
there.

**A course of antipsychotic holds it down** and cures nothing. While you are on it you
are slowed and permanently hungry; when it runs out, everything is twice as loud for a
minute. The question is not whether to treat it but which of the two you would rather put
up with.

Two things are worse. **Catatonia** takes movement away outright and passes on its own —
nothing treats it, and a course prevents it. And the course itself, rarely, **turns on
you**: a temperature, muscles locking, damage across the whole body, with no antidote.
Stop taking it and get cold.

**Delirium** is the other one, and it is not the same thing at all: acute confusion from
blood gone, an airway shut, sepsis or cold. It has no treatment because it is a sign
rather than an illness — fix the cause and it lifts.

## An artery is not a faster bleed

Three grades now. A light cut clots. A heavy bleed never does. And under both of them,
rarely and only in a limb or a neck, a vessel: **four and a half percent of your volume a
second**, against one for a heavy bleed.

It goes with the heart, it is brighter than the rest because that blood has not been
anywhere yet, and **it does not clot, ever**. A dressing over one soaks through and keeps
soaking. A bleed in a thigh blacks the leg out in six seconds and has you on the floor
inside thirty.

You never have to open a chart to know which one you are looking at. It **pulses** on the
body, brightening and dimming on the beat; it throws particles sideways in bursts instead
of dripping down; it flashes twice as fast on the corner figure, in a different red; and
the pressure screen for it is a different picture - a jet that arrives on the beat rather
than a pool with a run leaving it, wanting more of your weight and a longer hold, and
starting over from nothing the moment your hand lifts.

Two answers, and you already carry both: **a hand pushed hard into it**, or **a strap
above it**. While the hand is there nothing comes out at all.

---

## Going out

Not the same as going down. Going down is a clock counting towards a grave. A faint is a
body deciding it cannot keep your head supplied **while standing up**, so it stops standing
up - which fixes it.

Seven seconds later you are fine. Nothing is damaged, nothing is permanent, nothing needs
treating. It is this mod's only free warning.

What it costs is the seven seconds and **wherever you happened to be**. In a field it is an
inconvenience. On a ledge, in water, or in front of something that was already swinging, it
is the whole of the danger.

Three roads in: volume going, and steeper the emptier you are; pain on its own with no
blood lost at all - the faint at the sight of a broken arm, usually somebody else's; and
the bill for adrenaline. Getting up and running multiplies the chance.

**The screen goes completely black while it lasts.** You cannot see anything in a faint.

---

## What it looks like from inside

Losing blood used to hand you a bottle of blindness, and a rattled head handed you nausea.
Those were the same four potion effects for nine different states, so a body running out of
blood and a body with a fever looked identical from the inside.

Now the thing itself is drawn:

- **Volume going** closes the edges in and takes the colour out of what is left: grey and
  narrow from the outside first, then a tunnel, then nothing.
- **The heart** shows in that narrowing - everything dims and returns **on your own real
  pulse**, so a screen that has started beating quickly is a reading nobody had to take.
- **A hit head** will not hold the horizon, and does not bring it back to the same place.
- **Fever and sepsis** wash the room warm and soft.
- **Cold** is the opposite: pale, hard, and a shiver instead of a drift.
- **A faint, and anaesthetic**, are black.

None of it changes anything about the body; it changes what the body can see. It has a
strength slider and an off switch in the client settings, and the camera movement is a
separate switch from the picture - because that is the half that makes people ill.

---

## The splint stays on

A splint used to be a full stop: the bone was set, the item was spent, the stick vanished.

Now the stick **stays**. Two boards and two ties, drawn on the body over whatever else is
wrapped round the limb, on the chart, and holding the limb still for as long as it is
there: the bone is left alone whatever you are doing.

The clock on it is a bargain rather than a timer. Walking runs it down. **Running puts time
back on**, every second of it, and it takes longer again the next time you do it. There is
a ceiling, so a bad week still ends. When it runs out the splint comes off by itself and
the bone is done.

A splinted arm costs the quality of anything done with it; a splinted leg costs speed.
Neither is pain, so painkillers hide neither.

---

## Bones move

A bone can break and stay where it was, or the ends can move. The second is what people
picture when they hear the word, and you do not need a chart for it - the limb is the wrong
shape, and everybody can see it.

**A splint stops being the answer.** Strap a stick to a bone that is out of line and you
have held it precisely where it should not be. The mod does not refuse, because "no time,
no hands, splint it anyway" is a real decision - but it says out loud what will come of it.

Above both, the one that is also a wound: **bone through the skin**. It bleeds, it will not
close while there is bone in the way, and it is three times the road to infection. No
splint goes on it and nothing is set until a surgeon has cleaned it out and closed it.

### Setting one

Bare hands, three moves in order:

1. **Pull** along the bone and away from the body until the ends can pass. Not enough and
   they grind against each other. Too much and something between them tears - and in one
   case in five the end of the bone finds a vessel.
2. **Line it up** while the pull is holding.
3. **Let it down slowly.** The ends seat as the pull comes off, and a hand taken away all
   at once drops them together however they were lying.

There are three outcomes rather than two: set, seated out of line (nothing spoiled, try
again), or torn.

It is the most painful thing in this mod and **the only treatment the room can hear**. The
noise brings whatever is out there. A patient under the mask, or deep enough into the
morphine, makes none - which is the first reason to carry that mask that is not about
quality.

### Healed crooked

A bone left out of line knits out of line, and that part is **worth less for good**. It is
the only permanent loss of health in the mod: the bone is the wrong length, the limb walks
slower, and it aches on the weather louder than any straight one.

One way out, and it is the real one: a surgeon **breaks it again**, on purpose, so that
this time somebody can set it straight.

---

## Limbs come off blades

The mod could take an arm off with a blast and off with a saw. The gap between them was
where it actually happens: somebody with an axe.

It needs an edge - a zombie's fists do not do this and neither does an arrow - swung hard,
into a limb, and mostly into one already beaten past being a limb. Through a sound one is
possible and nearly impossible. What it leaves is the stump a blast leaves: open,
unstrapped, bleeding, and the arm on the ground beside you with a clock on it.

---

## Somebody is allergic to bread

Ten allergies are rolled at spawn and **seven of them are food**: fish, wheat, egg, milk,
honey, mushrooms, berries. The other three are in your own bag - antibiotics, morphine,
anaesthetic.

A drug allergy is somebody else's problem to remember about you. A food allergy is yours,
every time you open your own inventory, and nothing warns you or refuses the click. You eat
it, you gain the hunger, and then your throat starts closing: the same anaphylaxis the
wrong blood causes, and the same syringe answers it.

Your own is told to you in chat on the day it is rolled, and it stamps onto a bracelet.

---

## What a body keeps

Every injury in this mod used to heal back into the body it started as. Somebody who had
survived four amputations, a septic belly and a skull fracture was, once mended,
indistinguishable from somebody who had never been touched.

Parts remember three things now:

- **A knitted bone** is not weaker - but it is where the next fall lands, because whatever
  broke it is the thing you keep doing. And it **knows the weather**: an old break aches
  before rain, and aches indoors, because what it answers to is the pressure. Cold does it
  too. It costs nothing and cannot be treated.
- **A scar** is not skin. Less blood goes through it, which is exactly why the next
  infection settles where the last one did.
- **Stiffness**: a deep burn closes by pulling its own edges together, a wound closes
  tight, and a limb sewn back on does not straighten the way it did. A stiff arm is not a
  weak arm - it is one that does not go where it is put - so what it costs is the quality
  of anything done with it, and a stiff leg costs speed.

The counts never go down and only dying clears them. The stiffness does, and that split is
the whole design: the history ratchets and the disability does not.

### Working it loose

Take the joint out to the edge of what it will do and hold it there. Short of the edge
nothing loosens - being gentle with a contracture is how people keep one for life. Past the
edge something tears and it is tighter tomorrow.

The edge is not drawn anywhere and cannot be, because the only thing that knows where it is
is the person whose arm it is. They tense before they say anything, they say something
before it hurts, and they shout before it tears: **the whole instrument is their face.**

Three sets, one session per limb per day. On yourself it is worth half, because you stop at
your own pain.

---

## Other mods

None of these is a dependency. Each is detected at runtime, and a client or server without
it behaves exactly as it did before.

| | |
|:---|:---|
| **Sable: Ragdolls** | a downed player is dropped by the physics engine, lies where they land, and cannot get up until the downing ends |
| **Ragdollified** | the same bargain through a different physics mod; Sable is preferred when both are installed |
| **TaCZ** | a bullet is routed as a physical hit at the place it landed, even when the round carries vanilla's armour-piercing tag - so a rifle round can reach an organ or tear a heart instead of being spread over the whole body; some of them stay in, each calibre with its own shape and name, and have to be cut out with forceps |
| **Iris** | under a shader pack the whole-body pale and blue washes are skipped, because translucent whole-model layers go see-through in several packs; burns, bones, dead limbs and dressings stay |

---

## Commands

| | |
|:---|:---|
| `/triage info` | what state every part of you is in |
| `/triage hurt \| heal <part> <amount>` | for testing |
| `/triage bleed \| infect <part> <0-2>` | |
| `/triage fracture \| crack \| burn <part> <true\|false>` | |
| `/triage disease <cold\|poisoning\|fever\|sepsis\|concussion> <true\|false>` | |
| `/triage pneumo <part> <true\|false>` | |
| `/triage arrow <part> <count>` | leave arrows in a limb |
| `/triage bullet <part> <count> [type]` | leave bullets in a limb; type is a TaCZ calibre such as `762x39`, or a full id |
| `/triage press <part> <seconds>` | put a hand on a wound |
| `/triage airway <true\|false>` | block or clear an airway |
| `/triage arrest <seconds>` | stop the heart, or 0 to clear it |
| `/triage painkiller <seconds>`, `/triage reset` | |

`/tri` is short for the same thing.

---

## Configuration

`config/triage-server.toml`, server side, so it travels with the world. Part
health, where hits land, bleeding, blood loss, cracks, infection, burns, illness
and every penalty are in there.

On NeoForge the same file can be edited in game: the settings button on Triage in the mod
list opens every section with its comments and ranges. It edits the existing file and
shows the values already in it. Forge 1.20.1 and both Fabric trees still use the file.

Each subsystem has its own switch:

| | |
|:---|:---|
| `health.regeneration` | off — vanilla healing stops entirely, and only bandages, kits and surgery mend a body |
| `effects.minigame` | off — medicine works the moment it is used |
| `effects.downed` | off — back to dying where you stand |
| `effects.carry` | off — a downed player cannot be picked up |
| `effects.ragdoll` | off — a downed player poses instead of falling, with Sable: Ragdolls or Ragdollified installed |
| `diseases.shock.enabled` | off — losing blood stops being able to stop your circulation |
| `diseases.hypothermia.enabled` | off — cold blood clots like any other |
| `chest.enabled` | off — ribs never puncture anything |
| `effects.assess` | off — no reading other people without treating them |
| `effects.tags` | off — no marks, and no nameplate says anything |
| `effects.heartbeat` | off — a body low on blood stops being audible |
| `effects.airway` | off — a body on the floor goes on breathing by itself |
| `effects.cpr` | off — the clock running out kills, as it used to |
| `effects.pressure` | off — a bare hand does nothing for a wound |
| `effects.arrows` | off — arrows never stay in |
| `effects.guns.bulletsStick` | off — bullets never stay in |
| `effects.guns.bulletStickChance` | 0.25 — chance a round that hits stays in |
| `effects.guns.bulletTear` | 3.0 — damage a badly cut-out bullet does on the way out |
| `effects.organs` | off — there is nothing inside a torso to tear |
| `effects.heartStopChance` | 0.35 — chance that tearing a heart stops it instead of only bleeding |
| `effects.organBrokenBonus` | 1.0 — broken bone stops mattering to what is under it |
| `effects.pulse` | off — a heart is something you read off a chart again |
| `effects.breathing` | off — a chest stops being something you can read |
| `diseases.deafnessSeconds` | 0 — a blast leaves your hearing alone |
| `effects.blast` | off — an explosion goes back to being a big fire |
| `effects.blastWave` | off — a blast is fragments and heat and nothing else |
| `effects.temperature` | off — a forehead tells you nothing |
| `effects.ask` | off — nobody says anything |
| `effects.anaesthesia` | off — an operation says nothing about the patient again |
| `effects.dressings` | off — a bandage is an event again and nothing is tracked |
| `effects.necrosis` | off — a strap is a slow cost again, with no deadline |
| `effects.amputation` | off — a dead limb stays on the body for good |
| `effects.prosthetics` | off — a stump takes nothing |
| `effects.prostheticWearSeconds` | 0 — a fitted limb stays as good as the day it went on |
| `effects.phantomPain` | off — the body forgets |
| `effects.theatre` | off — a treatment is the same anywhere, with anything |
| `effects.placeQuality` | 0.25 — what the room is worth (×0.75 … ×1.25) |
| `effects.grimeInfection`, `effects.roomInfection` | 0.55 and 0.18 — the dirty instrument and the bad place |
| `effects.mending` | off — a treatment ends when it ends |
| `effects.mendingSeconds`, `effects.mendingTearChance` | 90 and 0.10 — how long it knits, and the risk of working it open |
| `effects.assist` | off — the second person in the room is scenery again |
| `effects.assistQuality`, `effects.assistAwakeCap` | 0.15 and 0.7 — what a pair of hands is worth, and the ceiling while held |
| `effects.fatigue` | off — a body weighs nothing after the first step |
| `effects.fatigueSeconds`, `effects.fatigueRecoverSeconds` | 90 and 45 — carrying until the arms go, and coming back |
| `effects.fatigueQuality`, `effects.fatigueCarrySlow`, `effects.fatigueHold` | 0.25, 0.5 and 0.15 |
| `effects.adrenaline` | off — a wound hurts from the tick it happens |
| `effects.adrenalineSeconds`, `effects.adrenalineRelief` | 25 and 0.8 — how long, and how much of the pain |
| `effects.adrenalineCrashSeconds`, `effects.adrenalineCrashBite` | 20 and 0.3 — and the bill |
| `effects.adrenalineShake`, `effects.adrenalinePulse` | 0.15 and 30 |
| `effects.epinephrineRounds`, `effects.epinephrineBleed` | 2 and 0.06 |
| `effects.epinephrineArrestChance` | 0.35 — a second shot on top of the first |
| `effects.morphineSeconds`, `effects.morphineStopsBreathing` | 75 and 0.4 |
| `effects.morphineDepresses` | off — a stronger painkiller and nothing else |
| `effects.naloxoneShockPain`, `effects.naloxoneShockChance` | 3 and 0.5 |
| `effects.referredPain` | off — they name the part the injury is actually in |
| `effects.chill` | off — a body opened in a blizzard is a body opened indoors |
| `effects.chillPerOperation`, `effects.chillDegrees` | 0.3 and 2.5 |
| `effects.chillHypothermia`, `effects.chillWarmSeconds` | 1.0 and 90 |
| `effects.coldCatching` | 0 — nobody catches anything off anybody |
| `effects.sneezeChance`, `effects.sneezeDamage` | 0.04 and 0.8 |
| `effects.pneumoniaAfterSeconds`, `effects.pneumoniaChance` | 180 and 0.01 — when a cold can settle |
| `effects.peritonitisDamage`, `effects.peritonitisSepsisSeconds` | 0.12 and 240 |
| `effects.schizophrenia` | on by default, ten in a hundred bodies; turn it off for a group that does not want it |
| `effects.delirium` | off — a starved head keeps its wits |
| `effects.whisperSeconds`, `effects.phantomSeconds` | 0 — silence, or nobody standing about |
| `clots.clotChance`, `clots.stillSeconds` | 0.00008 and 90 — how likely, and how long a limb must be still; a clot also needs a tourniquet, splint, break, blood loss, the floor or anaesthetic |
| `clots.clotClearSeconds`, `clots.embolismChance` | 240 and 0.002 — how long one lasts, and the chance it travels while the limb moves |
| `blood.stainSeconds` | 300 — how long a blood trail stays on the ground |
| `armour.aimedHits` | off — every hit goes back on the routing weights |
| `infection.enabled`, `diseases.enabled`, `burns.enabled` | off — those subsystems stop existing |

---

**Minecraft 1.21.1 and 1.20.1, on NeoForge, Forge or Fabric** · **MIT licensed** · English and Russian
