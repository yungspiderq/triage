# 2.2.0

A body model should not decide for the player how much of the body is simulated.

This release is mostly answers to people who asked for the same thing in different words:
let me turn this off, let me make this rarer, let me use the mod with the other mod I
already have. Where the answer was a number, the number is now a config key rather than a
constant. Where the answer was a compatibility problem, the mod now asks the other mod
what it is doing instead of guessing.

## Regeneration can be switched off

Asked by **Foresight18**, and the answer was no for eleven versions because natural
healing is not one feature in this mod - it is a path that everything else quietly leans
on.

`[health] regeneration` is now a switch, on by default. Turn it off and every source of
vanilla healing is refused before it can touch the body: food, potions, golden apples,
beacons, modded regeneration, all of it. What still works is anything a person does with
their hands - bandages, medkits, splints, surgery, saline, blood - because those call the
body directly and never pass through vanilla's heal at all. That distinction is the whole
point of the switch: passive recovery stops, treatment does not.

A world that turns it off keeps the healing scale and the chest penalty for the things
that still heal. Existing worlds keep the old behaviour until the key is changed, which is
what a default is for.

## The clot clock

Reported by **user_k2tteatq18xh9905**: cramps, clots and pulmonary embolism arrived fast
enough - inside five in-game minutes of ordinary walking - that they read as a tax on
movement rather than a consequence of injury.

The old rule asked only for stillness, and the stillness meter outlived the sitting. A
player who stood up and walked away kept a full meter for a while, and a clot could form
in that window with nothing else wrong with them. That is not a clot; that is a dice roll
with a medical name.

Three things changed:

- A clot now needs a reason. A tourniquet, a splint, a fracture or crack, at least five
  percent of the blood gone, being on the floor, or being under anaesthetic. A healthy
  body that has merely stopped moving does not make one, however long it stands there.
- It needs **ninety seconds of real stillness** (`[clots] stillSeconds`). The meter still has
  to fill, but it now empties while the player moves, and a clot that is already sitting
  in a limb has a much shorter life: four minutes by default (`clotClearSeconds`, down
  from seven).
- The base chance is down from 0.0016 to 0.00008 per second, and while the body is moving
  it is multiplied by 0.1 for as long as the meter is winding down. Embolism chance is
  down from 0.035 to 0.002.

The design is unchanged: a clot is what a limb does when it has been left alone, and the
danger is standing up. It is no longer what a limb does while its owner walks to the
next village.

## Iris

Reported by **user_k2tteatq18xh9905**: with a shader pack on, the semi-transparent
whole-body wash turned the player and every other casualty see-through.

That wash is one translucent layer drawn over the entire model, and Iris composites that
layer differently from vanilla in several packs. The mod now asks Iris whether a shader
pack is in use, once every couple of seconds, through reflection - Iris is not a
dependency and a client without it never loads a class that mentions it.

Under a shader pack the full-body pale and blue tints are skipped. Everything local stays:
burns, bones, dead limbs, dressings and steel. Those are small pieces drawn on the part
they belong to, and they survive the compositing that the full-body wash does not. The
result is that a shader player loses a distance cue and keeps their arms.

## Guns

Asked by **bomb_man**, and the request had four parts: TACZ support, bullet penetration,
thorax and heart hits, and a heart that can stop.

TACZ armour-piercing rounds carry vanilla's `bypasses_armor` tag. To vanilla that means
"do not subtract armour points"; to this mod it meant "there is no point of impact", and
every rifle round was spread evenly across seven body parts instead of landing where it
went in. Damage routing now recognises a TACZ projectile by its entity package and its
damage-type message id, and sends it down the physical path even when the tag says
otherwise. The round still penetrates armour - it just does so at the place it hit.

That gives the rest of the request for free: a physical hit has an impact point, so a
round that lands on the chest can reach the organs, and a round that lands on the heart
can tear it. No TACZ class is compiled against; a future build with the same package and
message id is still a bullet.

## Bullets stay in

A round can now lodge. About one in four that hits stays in the body - more often than an
arrow, because a round that has already gone through somebody has spent most of what it
had and stops in the meat.

The wound is the arrow's problem with a different way out. A shaft stands out of the skin
and comes back along the hole it made; a bullet is under the skin, so there is nothing to
take hold of. The wound has to be opened and the round picked out with forceps - the same
job as fragments, played with the same instrument, and priced separately: a bad cut-out
tears more than a bad pull, because the jaws are working inside tissue that has already
been torn once.

The game itself now looks and reads like that job. The pieces in the wound are rounds -
rounded nose, copper driving band, lying along the track they came in by - and the screen
says **Pick out the bullet** rather than Pick out fragments.

Until it is out the part will not clot, will not mend and cannot be dressed. The chart
calls it a bullet rather than an arrow, the film shows it, and `/triage bullet` puts one
in a limb for testing. Three new keys live under `[effects.guns]`: `bulletsStick`,
`bulletStickChance` (0.25) and `bulletTear` (3.0).

No TACZ class is named here either; the round is recognised the same way the damage
routing recognises one. TACZ has no Fabric build, so the mechanic is live on NeoForge and
Forge and dormant on Fabric.

## Every round has its own calibre

A lodged round used to be one blunt cylinder with one name, no matter what had been fired,
even though TACZ ships twenty-five kinds of ammunition from 9×19mm pistol rounds to an
RPG-7 rocket. A pistol round and a rifle round do not look alike under the skin, and the
screen that asks somebody to fish one out should not draw the same stub for both.

Each TACZ round is now kept in the wound by its own id: `9mm`, `12g`, `762x39`, `50bmg`,
`rpg_rocket` and the other twenty. Every kind has its own nose profile (round, spitzer,
flat, hollow, pellet, rocket), length, width, jacket colour and driving band. The operation
title and the X-ray name the calibre, `/triage bullet <part> <count> [type]` stages a
specific round, and the tab suggestions list all twenty-five. The old command without a
type still parses and still means the generic round.

Old worlds and configs load without migration: a part with a count but no names is read as
generic rounds and filled in as new ones arrive. A calibre from another gun pack keeps its
own id and borrows the nearest shape by name instead of becoming a ninth 9mm. TACZ stays
optional - the id is read by reflection and no TACZ class is compiled against - and on
Fabric, which has no TACZ build, rounds still do not lodge.

## The heart stops

A torn heart used to be a very fast internal bleed and nothing more. It is now also a
chance - 35 percent by default, `[effects] heartStopChance` - that the heart stops
rather than merely emptying into the chest. When it does, the player goes down into
arrest through the same path as any other cardiac arrest: CPR, compressions, adrenaline
and a surgeon are the answers, and the normal arrest clock applies.

The chance is only rolled when downed states, CPR and a non-zero arrest time are all
enabled. A server that has turned CPR off gets the old behaviour - a torn heart is still
an internal bleed, which is still usually fatal, just not scripted.

## Ragdolls, plural

Asked by **Meltagrounds** and **cbjpok**: Ragdollified support.

The ragdoll backend was written around one mod. It now asks for Sable first and
Ragdollified second, both by reflection, both behind the same `[effects] ragdoll` switch.
If neither is installed, nothing changes: the pose still changes and nothing falls over.
If both are, Sable wins because that is the one the existing fall-damage measurement was
built against. A downed player on Ragdollified is dropped, lies where they land, and gets
up through the same code path as before.

## Longer blood trails

Asked by **user_k2tteatq18xh9905**. A stain used to dry in ninety seconds
(`[blood] stainSeconds`), which is shorter than the walk back to the body. The default is
now five minutes, and the cap on live stains has gone from 500 to 2000 so a real fight
leaves a real trail instead of a budget. Both are still config keys; a server that wants
the old look can have it.

## Schizophrenia stays on

Asked by **user_k2tteatq18xh9905** to stop inflicting it on worlds that did not ask, an
earlier draft of this release made it off by default and dropped the rate to two percent.
That was the wrong reading of the request: what was asked for was a switch, not a
different default. This release adds every switch and changes no defaults.

`[effects] schizophrenia` is on, and the share of bodies that have it is ten percent, as
it has been since the condition was added. A group that does not want it turns it off; a
group that does keeps the world it already had.

## Settings in the game

The three hundred-odd keys could only be read by finding a file. On NeoForge the mod now
registers `ConfigurationScreen` through `IConfigScreenFactory`, so the settings button on
Triage in the mod list opens the whole spec: every section, every setting, its comment as
the tooltip, and the range it accepts. Nothing new is stored and nothing is overwritten -
the screen edits the same `triage-server.toml`, and a value already in it is shown rather
than replaced.

The screen is client code and is registered in a client-only mod class, so a dedicated
server never loads it. Forge 1.20.1 and both Fabric trees still need the file.

Every setting has an English label; the section names and the gun keys have Russian ones,
and the rest falls back to English.

## What this is not

There is no 1.21.10 build in this release. 1.21.10 changes enough of the client rendering
path that it is a new port rather than a version bump: the layer, model and shader APIs
this mod draws through all moved, and the 1.20.1/1.21.1 trees share a rendering half that
would have to be rewritten twice. Publishing a jar that had not been run would be worse
than saying so. The four current files - NeoForge 1.21.1, Forge 1.20.1, Fabric 1.21.1 and
Fabric 1.20.1 - are built from the same sources and carry every fix above.

The settings screen is NeoForge-only for now: the other loaders have no built-in screen,
and a hand-written one for three different GUIs is a separate job rather than a line in
this release. Lodged bullets need a gun mod, and TACZ has no Fabric build, so that
mechanic is live on NeoForge and Forge and dormant on Fabric.

# 2.1.0

One measurement is not information. Two are.

For eleven versions this mod could say what a body **is** and never once what it is
**doing**. Every reading overwrote the one before it: take a pulse and the last pulse was
gone - which means the mod was throwing away the only number that ever matters, the one you
compare against.

A pulse of 112 means nothing on its own. A pulse of 112 on somebody who was at 90 four
minutes ago is bleeding somewhere, and there is no wound on the outside to find.

## Fixed: 1.20.1 would not start at all

Reported by **Sleeper_Creeper**, and it was not one bug but three.

The mod opens two fields nothing else will open: a village pool's template list (there is no
other way to add a house to a village) and a renderer's layer list (there is no other way to
replace a layer). On 1.21 an access transformer opens them. On 1.20.1 ForgeGradle never
applied one, so they were opened by reflection - **by name**.

A field name is one thing while a mod is being written and another when it is played. Forge
1.20.1 calls `templates` `f_210560_` at runtime; Fabric 1.20.1 calls it `field_16680`. A
lookup by name therefore worked on exactly one machine in the world: the one the mod was
written on.

Both lookups sat in a static initialiser, so this was not a missing feature. It was a
`NoSuchFieldException` and a mod that would not load.

Which has an ugly consequence: **Forge 1.20.1 and Fabric 1.20.1 have not started since
1.9.0.** Both published files crashed on startup for everybody but me. The third place is the
same mistake in Fabric 1.20.1's client code - nobody ever reached it, because the game died
earlier.

The funny part is that the mod already knew. The explosion radius is read **by type**, and
its own javadoc says why: "the name differs between compiling and running and the type does
not." Two other places went by name anyway.

Now everything goes by type. A type cannot be remapped: a pool holds one `ObjectArrayList`
and one `List`, a renderer holds exactly one `List`, and that is enough under any names.
Verified by running the same lookup against the real production-mapped Forge 1.20.1 classes -
it finds `f_210560_`, `f_210559_`, `f_115291_`, `f_46017_` - and against Fabric's
intermediary ones.

And none of these fields can take the game down any more. If one is missing: a line in the
log, and the mod carries on - with no clinic in the villages, or with armour drawn the
vanilla way. Neither is worth a crash.

## The observation sheet

Every reading anybody takes off a body now goes on a page: what, how much, when, and whose
hands. Pulse, temperature, breathing off the stethoscope, and the pain score out of ten -
that last one was never written down at all until this version. It was said out loud and
thrown away.

Open it from the chart: the three lines in the top corner. Four of each kind, newest first.

The third column - the name - is half the reason the sheet exists. **The second medic to
arrive can see what the first one already did**, and not do it again. Handover finally
works.

Realistic hides none of it. Every line on that page was earned by somebody doing something;
the mode hides what nobody has found out, not what somebody has.

## The arrow

Once there is something to compare against, the chart prints an arrow beside the reading,
and the mod says the difference out loud as you take it: *Pulse: +22 in 4 min.*

The difference, never what it means. A climbing pulse is also a frightened person, a person
who has been running, and a fever on its way up. Which one it is, is the player's job; the
mod does the arithmetic and stops. That is the same bargain the whole realistic difficulty
makes, applied to a number.

And it is the only finding in the mod whose price is **patience** rather than an item or a
minigame. A direction cannot be bought, stolen, or read off a person. It costs a second
visit.

## What it catches

A torn organ has bled inwards since the version organs went in: silently, never clotting,
and nothing but a surgeon ends it. There is nothing to see from outside - no blood on the
floor, no wound. Until now the only way to find one was to open somebody up.

Now a rising pulse finds it. The mechanic did not change by a line. It finally has an
instrument.

## The table starts writing things down

A ward is not a place with better medicine in it. A ward is a place where somebody writes a
number on a page every so often, and that is the whole difference between a hospital and a
room with a bed in it.

A patient lying beside a medical table now has their pulse and temperature taken
automatically, every forty seconds, quietly - nothing in chat, the lines simply appear on
the sheet. Ten minutes in a clinic and the sheet is a column of numbers going the wrong way.

It is also the **first reason in this mod to carry somebody somewhere**. A clinic used to be
a building with supplies in it, and supplies travel. A place that measures for you does not.

## Four places where realistic was not quite true

All four were found by being asked a plain question - what actually differs between the two
difficulties - rather than by anything breaking. The mode was described accurately
everywhere except in the code that draws it.

- **The figure in the corner knew everything.** The HUD read the whole body: it coloured
  itself by the truth (a leg with a crack nobody had felt for was already a different
  colour) and named diseases outright, while the chart honestly printed "not examined". The
  corner now reads what the chart reads - and in realistic the diseases never arrive at all,
  so there is nothing left to name.
- **The X-ray never reached the chart.** You could photograph a shattered bone, see it on
  the film, close the screen, and the part was "not examined" again. A film now establishes
  bone and metal across the body: exactly what a film shows.
- **The X-ray sent the whole body to the client.** Organs, infection and clots went over the
  wire - none of which a film can show. It now goes through the same redaction as everything
  else: what the film showed, and nothing else.
- **A finding never aged.** Its age was printed in a hover tooltip, which is the one place
  somebody reading a chart in a hurry will never look. An old finding is now printed faintly
  in the row itself. Not removed: an old finding is still the last thing anybody knows, and
  throwing it out would punish the one person who did the work. What goes is the confidence,
  not the fact.

# 2.0.0

One setting, and it splits the mod in half.

`[difficulty] realistic` is off by default, and while it is off **nothing changes**. Arcade
is the mod as it has been for eight versions: open the body chart, read "left leg, two
breaks, infected, 4 of 14", go and treat that.

Turned on, it changes exactly one thing - **where that knowledge comes from**. The body is
identical: same injuries, same rates, same treatments, same minigames. What goes is the free
answer.

## The chart stops being a readout

It becomes what a chart is in life: **a form you fill in**. A part nobody has examined reads
as a blank. Not zero, not healthy - blank.

The difference between "I know there is nothing there" and "I did not look" is the whole of
this mode. In arcade that difference does not exist, because there everything is known at
once.

## Why this became possible now

Because the instruments were already built, one version at a time, on a principle written
into their own javadoc: **the reward is a fact, not a repair.**

A pulse is a fact about a body - fast and thready on somebody with nothing showing is
internal bleeding. Asking where it hurts is the only channel that comes *out of* a patient,
the only one that stops when they do, and the only one that can lie. Asking for a number out
of ten is an opinion, and an opiate lowers it. Pressing on a burn is a finding that is an
**absence**: the patch that does not answer is through the skin. A thermometer is a figure
off an instrument. An X-ray is the only certain answer about bone.

Eight versions of building ways to find out what the chart was giving away. This mode is
**switching the chart off** and leaving those.

## What is plain

Whatever anybody walking into the room would see, in either difficulty: blood and how hard
it is coming, a dressing, a strap, a splint, a limb that is not there, a prosthetic, a burn
on the skin, a **displaced** fracture, an open one, a limb gone black to necrosis.

The rule behind that list is one question asked of each fact: would somebody know this
without doing anything? A missing arm, yes. A leg lying at an angle it does not bend at,
yes. Two cracked ribs under unbroken skin, no - and that "no" is the mode.

And the rule that matters more: **it never hides what is plain.** A black limb, a bone
through skin and blood coming out of somebody are facts about the world rather than entries
on a form, and a difficulty setting that pretended otherwise would be a difficulty setting
about lying to people.

## Feeling for a break

A new action, and the answer to the question the mode is built around. A bone out of line
can be seen across a room; a bone that broke and stayed put leaves **nothing** on the
outside, and there are exactly two ways to find one: a film, or a hand.

Three places along the bone, one press each, and the whole skill is how hard. Lighter and
the hand is on skin and learns nothing. Harder and you have hurt them for nothing - and on a
break that is still fresh, a press like that **moves it**, turning a bone that would have
healed straight into one that has to be pulled back into line first. Between the two there
is a band, and hitting it three times running is what an examination is.

It is the first reading here the patient pays for. A pulse costs them nothing and a question
costs them nothing; this is pressure on something already broken. The examination can cost
more than the finding is worth, and knowing when that is true is the skill.

On yourself it is worth less. Your own hand stops early, every time, and that is not
cowardice - it is why this is a job for somebody else's hands.

## The stethoscope

A new item, and the only finding in the mod with **no free way in at all**.

Everything else has one: a limb is a shape, a burn is on the skin, a bone grates under a
hand, blood that has gone somewhere drives a pulse two fingers can find. A collapsed lung
gives you none of that. The chest looks like a chest, and what the patient says - *I cannot
breathe* - is the same sentence they say with a mouthful of blood.

Four places, a second at each, and hold the bell still. The finding has the same shape as
the burn press: **an absence**. Air moving on both sides, or air moving on one.

## What writes itself down

The instruments that were already here now remember. A film closes every bone and every
piece of metal in one go, which is what the machine is worth its weight for. Pressing a burn
closes its depth. An operation closes what it exposed. Hands that have been inside a part
know what is inside it.

And a finding **goes stale**. It carries a time - the part tooltip says how long ago somebody
felt along it - and a blow to a part erases everything known about it. The leg somebody
examined two minutes ago and found sound is not the leg that has just fallen off a cliff.

## Looking somebody over

Realistic had a hole in the middle of its loop and it was mine: standing over somebody you
have seven parts and one way to ask about each, so the honest play is seven palpations in a
row. That is not thoroughness. It is four minutes of pressing on a person who is bleeding,
and no medic has ever worked that way.

The first thing anybody does to a casualty is go over all of them at once and find out
**where** rather than **what**. Now that exists, and the mod finally does the thing it is
named after.

One pass tells you how bad each part is - exactly what looking at somebody tells you. It
does not tell you why: a leg in a bad way could be a break, a bleed, a burn under the cloth
or a limb that died under a strap. That is the work the hands and the instruments do, and it
starts after this.

The whole game is pace. A band moves down at the speed this is done at and the job is to
stay in it. Hurry and your hands go past the quiet leg - the one nobody is complaining
about, which here as in life is the one that kills them an hour later. And the mod says so:
*your hands went past something. You do not know what.*

One sweep turns seven questions into one.

## And two more effects

**Listing onto the good leg.** The only thing on this screen with a **side** to it.
Everything else is a state - how much blood, how hot, how tired - and a state has no
direction. A body that leans is naming the leg it will not stand on. In realistic that is a
finding arriving through the floor instead of through a form, and it is the only one that
does.

**Grey on standing up.** The only effect here about a **transition** rather than a state,
and the one everybody has had: blood that was level with your head a second ago is now a
foot and a half below it, and a body short of it cannot catch up. Stand from lying or from a
crouch while you are down on volume and the world goes grey and narrow for a second or two.

## Five more things you can see from inside

In arcade they are illustration. In realistic they are the **channel**: a person feels
themselves rather than reading themselves, and this is what they feel with.

One rule for all five: an effect that does not mean a particular thing is decoration, and
decoration on a screen somebody is trying to read a body through is worse than nothing.

**Floaters.** Specks at the edge of vision when blood is short or the head has taken
something. The sign that arrives *before* the walls close in - by the time the tunnel is
obvious the decision has been made for you, and this turns up while there is still a
decision. Anybody who has stood up too fast knows what it means without being told.

**The chest.** Two bands, top and bottom, closing and opening on the breath - at the rate
the mod already works out for that chest. Not a speed of its own: an effect running at its
own pace is an effect that disagrees with the readout beside it, and nine versions have gone
into making those agree. A collapsed lung, smoke, a wrecked ribcage: the bands barely open.

**Blood in the eye.** Not a tint and not a vignette. It runs from above, goes where the
shape of a face sends it, and **dries** rather than fading. The only injury here whose first
cost is that the person it happened to cannot see what they are doing about it. A dressing
clears it; nothing else does.

**Shivering in bursts.** Cold used to be a constant tremble. Now it is a run of it, a pause,
another run. Inside a burst it is as fast and as regular as it was - that part was right -
but a body that shivered without stopping is a body with nothing left to shiver with.

**The head going.** What a rattled head does that a slow drift does not: it goes, suddenly,
and comes back. Rare, short and sharp - once every twenty-odd seconds - and the lesson is
the right one: stop moving for a second rather than fight it. The drift alone never taught
that.

## Three complications, and none of them another fever

There were twenty-two illnesses already, so the bar for a twenty-third is not "one more
thing that makes you hot". Each of these three has a shape none of the others has: its own
cause, its own way of being found, its own answer. And two of them are caused by
**treatment** rather than by dirt - an injury the medic made is the only kind that makes
somebody read their own last decision again.

**Lockjaw.** The only thing in this mod that cares what *shape* a wound is. A gash left to
go bad goes septic; that was already true. A deep narrow hole from an arrow or a fragment,
closed over dirt by a dressing, does something else. It is silent, and it is silent for
exactly as long as it is treatable; when the silence ends, a course stops working, for
good. The one tell in all that time is not a reading and not an instrument - it is a
**sentence**: ask the patient, and somebody whose jaw is stiffening answers slowly. No tool
in this mod finds it. A medic who never talks to anybody never sees it at all.

And when the silence ends: spasms. A spasm **interrupts a treatment from the inside**, and
it is the only thing here that does - everything else that knocks hands off somebody is
somebody hitting them. A patient nobody can finish working on is a new problem, and it is
the correct one.

**Crush injury.** The most purely medic-made injury here, and the only one caused by
**doing the right thing too late**. A limb with nothing going through it fills up with what
a living limb flushes away, and the moment the strap comes off, all of it reaches the heart
at once.

Not an argument for leaving the strap on - that kills the limb. Not an argument for taking
it off faster either. It is an argument for **fluid first**: a patient who is not short of
volume takes what comes back diluted, and nothing happens. The same bag of saline, hung
**before** rather than after - and it is the only place in this mod where an infusion is
something you set up in advance.

**Relapsing fever.** One bottle is one dose, and a dose is not a course. Sometimes the
fever lifts, the patient gets up, and it is back in waves a few minutes later. What makes
it worth having is what it does to the instrument: a fever that cycles is **normal at the
bottom of its cycle**. A reading taken then is honest, accurate, and says there is nothing
wrong. Nothing else in this mod can be right and wrong at the same time. Measure twice, ten
seconds apart.

## The colour of a person

Every finding in this mod is **fetched**: walk over, kneel down, put something on somebody.
The two a real medic uses before any of that, this mod has never had - the colour of the
skin and the colour of the lips. Both are visible from the other side of a room, before
anybody has been asked anything.

Now they are on the model. **White is volume. Blue is air.** They cannot be confused with
each other, they are never the same emergency, and they want opposite things done first. In
realistic they are the only two facts a medic gets for free - and that is enough to know who
in a room to go to first.

## The room going quiet

Every symptom until now has been something to look at. Hearing in this mod did one job -
being blown up - and because of that, the most reliably reported sign in all of first aid
went unbuilt: **the sound going away**.

Ask anybody who has fainted what happened first. Not "my vision narrowed". "The voices went
far away."

It is also the only symptom here in a channel of its own. By the time somebody is that far
gone the screen is already full - the edges are in, the specks are up, the grey is over all
of it - and a player who has stopped reading the picture will still notice the world go
quiet.

## Sweat that gets into your eye

A fever does not put droplets on the inside of your eyes, and nobody has ever seen their own
face; beads on the screen would be a film effect rather than a symptom. What a hot person
actually gets is the one thing sweat does to the person producing it: it runs down, it
arrives in an eye, and it **stings**. For a second they squint, and for that second they
cannot see what they are doing.

Which is exactly where a symptom in this mod is supposed to land: it tells you nothing, it
**takes something away**, and what it takes is what somebody tying a knot in a bandage needs.

## A weapon that stayed in

There was a line in a comment in this mod: only arrows lodge, and "a sword does not stay in
anybody". That is true of ninety-nine swings out of a hundred and false of the hundredth -
and the hundredth is worth more than the rest put together, because three things happen at
once that happen nowhere else here.

**The thing in the wound is holding the wound shut.** Not a figure of speech: the wound is
not bleeding because the thing that made it is still in the hole. The bleeding in that part
stops, which reads exactly as wrong as it should. Nothing can be bandaged over it, nothing
clots around it, the part will not mend. So the injury is the **removal**, and that is the
whole decision: hands now, or a kit in a minute. Hands leave the worst bleed in the mod. A
kit leaves one a dressing answers. You are asked at the worst possible moment, every time.

It is also the one piece of first aid everybody has heard and nobody believes: **do not pull
it out.**

**Whoever swung it does not have it any more.** The only disarm in the mod, and it happens
to the winner of the exchange. A zombie with an iron sword can be taken apart by one unlucky
swing. So can a player.

**And you can see it from the other side of a field.** Every other free finding here is a
colour or a posture. This one is an object, standing out of somebody who is still walking
around. Nobody has to be taught what it means.

The odds are long and it needs a heavy blow, so armour mostly prevents it and no armour
mostly does not. Every step moves it; sprinting costs four times as much. And the X-ray
finally does the thing X-rays were invented for, and says: *metal, and a great deal of it.*

To see it without waiting for the hundredth swing: `/triage blade <part>` puts whatever is
in your hand into it.

## A chest you can hear

The mod has spent ten versions making a chest worth **reading** and never once made one
worth **hearing**. The first thing that happens when you kneel next to somebody is that you
can hear them.

Now a chest that is working badly makes the noise it makes. Quietly, at about a body length,
which is the range at which it is true. What you hear is only **that** something is wrong -
never the rate, never the cause. A wet chest, a chest that cannot finish a breath and a
chest working far too hard all sound different.

This does not devalue the stethoscope; it promotes it. The instrument stops being how you
find out a chest is bad and becomes how you find out *which* bad it is, which is what a
stethoscope is for.

And the worst case makes no sound at all. That is not a gap: a chest that has stopped is
silent, and somebody who has learned to listen for the noise will one day notice it missing.

## A patient who will not hold still

Everything that could stop an operation came from outside the room: somebody hits the medic
and the screen closes. The patient - the only person present with an opinion about what is
being done to them - has never once objected. Awake. With nothing in them. With a saw
against their leg.

That one unwritten line quietly made three of this mod's own systems decorative. Ether
existed and nobody needed it. A second pair of hands existed and did nothing a first pair
could not. Painkillers were for the patient's comfort, and a patient's comfort is not
something a game can make anybody care about.

Now they pull away, and all three became the difference between a job that finishes and a
job that does not - without any of the three being changed.

Only on jobs that open skin, and scaled by how much pain they are actually in. **Ether, a
dose, or a second pair of hands, and it never happens.** Nothing is spent when it does: what
it costs is the time, and the walk back for whichever of the three you did not bother with.

## How it is built, and why that way

The server keeps the truth and sends the client a body with everything unestablished taken
out of it. Otherwise the mode is worth exactly what a modified client says it is worth.

What is taken out is replaced by **the value that means nothing is wrong**, never by a
sentinel. That matters more than it looks: any drawing code that forgets to ask whether a
fact is known draws a healthy body. Which is wrong, and safe. Only the chart can tell "no
breaks" from "nobody looked", because the findings travel beside the body.

Findings live **on the patient**, not on whoever found them. That is how a chart works: it
travels with the person, and the medic who arrives second reads what the first one wrote.

Your own body is never redacted, and that is not an oversight. The symptoms, the ringing,
the leg that will not take weight all read that body, and taking away what a person feels is
not what this mode is about. Your own chart simply prints something else: how bad each part
is and how much blood you have left stay, and a cracked rib stays a chest that hurts until
somebody puts their hands on it.

## Arcade did not change

Not one line. The new action and the new instrument exist there too - you can feel along a
bone in arcade, and somebody will, because that is what hands are for - but what they learn
is what the chart already said. That is the only difference, and it runs one way.

# 1.9.0

Four systems, and all four are about one thing: **finding out what is actually wrong.**

## A clot

Everything else in this mod arrives on the end of something - a blow, a blade, a fire, the
wrong dinner. This is the opposite of an event. **It is what a body does to itself when
nothing is happening to it:** blood that is not moving sets, and what stops it moving is a
strap, a splint, a broken leg, and an hour on the floor waiting to find out whether
anybody is coming.

Which makes it the first injury here **caused by treatment**. Not by a mistake - the
tourniquet was the right call, the splint was the right call, and so was the stillness
that let them both work. It is the first time this mod charges for a correct decision.

The leg is not the danger. It swells, it is warm, it aches, and that is all it will ever
do: nothing to see, nothing to put a hand on, and a quiet icon on the chart that nobody
would cross a field for. The only way to find it is to **ask the patient where it hurts.**

### What it is waiting for is you to stand up

A clot that has been sitting in a still limb goes when the limb starts working. And it
does not stop in the leg.

What arrives in the chest is something solid where the air was going: **the breathing goes
first** - measured on a live server at thirty and laboured - the heart follows it up, and
the chest comes apart faster than anything in this mod except an artery. No bandage, no
drip, no course of anything. One drug - the **blood thinner**, a new item - breaks it up
rather than treating the damage, and it has about a minute and a half to be found in
somebody's bag.

So the danger is not the injury. The danger is the recovery.

## Six operations instead of one

The surgical kit was one item, one screen and **one game for six different jobs**. A limb
beaten to nothing, a spleen with a hole in it, a burn that needed a knife, a skull that
needed opening, a leg with the bone outside it and a bone that healed in the wrong shape
were all "use the surgical kit", and all of them were the same two minutes of clamping
bleeders.

That said the wrong thing twice: that these are the same problem, and that the answer to
all of them is the same hands doing the same thing.

| | |
|:---|:---|
| **Wash it out** | the bone has been outside. Not a repair - the end of a race against what came in with it. One bottle; drive the grit over the edge |
| **Take the pressure off** | a skull or a spine. A hole: not on the crack, not on a vessel, through the bone and not a millimetre past |
| **Sew the tear** | on a surface that moves every time they breathe. The skill is **waiting for the pause** |
| **Cut the dead off** | a layer at a time until the patch **answers with blood**, which is the only sign there is that you have reached living tissue |
| **Open it and stop it** | the original operation: a fight for the view, the field filling, the vessels one at a time |
| **Break it again** | it healed crooked. Saw along the line and close the wedge squarely |

Which one is decided by `SurgeryJob`, and **both** sides read it: the screen that picks the
game and the server that applies the outcome. They cannot drift - nobody plays one job and
is handed another.

And a leg with the bone outside it and a burn on the same leg is now **two operations**,
because it is two operations.

## The pulse stops printing itself

The chart used to show a live pulse to anybody who opened it. Walk up to a stranger and the
screen handed you a heart rate nobody had ever felt for.

That made taking one worth nothing, and it made the second medic to arrive repeat
everything the first had already done.

The chart now shows **the notes**. Until somebody has felt for one the line is empty: *not
taken*. A pulse that was taken goes on the notes with the name of whoever took it, and it
**goes stale**: after forty-five seconds the chart keeps the figure and says how old it is.
A pulse from four minutes ago is a fact about four minutes ago, and on a patient who is
bleeding that is the difference between a number and a lie.

A temperature measured with the instrument goes on the notes too - as the figure rather
than the word. And a film leaves a line saying a picture exists.

## The X-ray

The one thing in this mod that **looks inside somebody**, and the only reading with no
opinion in it anywhere. It asks the patient nothing: it cannot be fooled by morphine or a
concussion or the patient being unconscious, and it does not care how steady your hands
are. What is on the film is what is in the arm.

It shows **bone** in every state the mod has - sound, cracked, broken, out of line, healed
crooked, including the cracks nobody can feel and the ribs and skulls that have no outside
to look at - and **metal**, which is every fragment still in somebody, counted.

And nothing else at all. No bleeding, no infection, no clot, no torn spleen: **a film of a
man dying of a ruptured organ is a clean film.** That is not a limitation anybody had to
invent, it is the most important thing to know about the instrument, and anybody who learns
to trust the picture instead of their hands will kill somebody with it.

Its screen is black rather than printed on the usual cream card, because a chart is what
somebody believed and a film is what is there.

The machine is heavy and stays where you built it. That is the trade: everything else a
medic owns travels, and finding out what is really broken means bringing the patient to the
machine.

## The medic's house

The village clinic was a nine-by-nine box - cobble plinth, plank walls, a stepped pyramid of
planks for a roof - and inside it one bed, a cauldron and the table. It worked, and it read
like a shed. Nothing in it said what the building was for, which for the one building this
mod adds to the world is the whole job.

It is now a clinic. **Two beds** rather than one, with an aisle between them and a lamp on a
post at the height of the person being looked at. **A bench along the south wall in the order
anybody would use it**: water to wash in, the table to work on, a stand for brewing, a barrel
of supplies - a couple of bandages, tags, string, sometimes a splint. **A stone floor under
that bench and planks everywhere else**, which is the oldest thing a clinic does to a room.
**A real gabled roof** of stairs with eaves that overhang, and a flat one with a parapet in
the desert, because a pitched sandstone roof in a desert village looks like somebody else's
house dropped in the street. **Windows at head height** on both long walls with a post
between them, so the facade has a middle.

The five village styles are now generated from one plan by `_tools/clinic.py` rather than
being five files drifting apart: a change to the layout is a change to all five.

## The guide

Fourteen sections, eighty-two pages, forty-six with something to run. The new heading is
**the operating table**, with a page for each of the six operations, and each of them can be
tried over nobody.

# 1.8.2

One fix, and it turned out to be larger than it looked.

## The thermometer could not give a figure to anybody, ever

You play it perfectly and the line under the reading says "held long enough and it would
have given a figure". It was held long enough. Nobody has ever got a figure out of it.

Three reasons, and the first is arithmetic.

**It was reading the wrong number.** The question "was it shaken down?" was asked at the
end and answered from the height of the column - and the column is reused during the hold
to show the new reading climbing. So by the time anybody asked, the thing being divided was
the new reading rather than the old leftover, and the test ran **backwards**: the longer it
was held - the better the reading, the more careful the player - the higher that column
stood and the more certain the game became that the thermometer had never been shaken.

Run out over every hold and every starting leftover, **the old code could return at most
0.300**, and the server wanted exactly 1.0. The fact is now remembered at the moment it
happens.

**The quality was being multiplied by the steadiness of the hands.** The room, the
instrument, the assistant, fatigue, sleeplessness - all of that multiplies into the quality
of any treatment. But a reading is not a treatment: a tidy room and a rested pair of hands
do not move a column of mercury. A flawless reading taken in a field still could not reach
one. The thermometer now gets what the screen earned and nothing else.

**And it was compared against one exactly.** A float that has been through a packet and a
clamp is a poor thing to ask equality of, so the threshold now sits just under.

The second line stopped lying while it was at it: if the column really was somebody else's,
that is what it says now, rather than telling you to hold it longer.

A figure comes out after **4.7 seconds** in the fold.

# 1.8.1

Two fixes in the fragment game, and eyes that close on their own.

## The forceps moved a centimetre

Here is why: the instrument only moved **while a button was held**. That is the wrong half
of the gesture - a player moves the mouse, sees an instrument that does not follow it, and
draws the only available conclusion, which is that the thing is stuck an inch from the
wound. It was not stuck. It was waiting to be dragged, and nothing said so.

Now the mouse aims it and the button closes the jaws: the same division of labour every
other pointing device in the game uses.

And with it, **the reach is drawn**. A faint arc swept round from the mouth of the wound,
because a rule that stops the job should never be a surprise - and with fingers that arc is
visibly, humiliatingly smaller. There is more of the reach as well: what was being held
back as margin was room the instrument needed.

## Fragments came out with bare fingers

Also found, and it was arithmetic rather than intent. The deepest fragment was placed
**past the reach of fingers** - correct as far as it went. But closing on a fragment does
not require the tip to be on it; anything inside the width of the grip counts. The gap was
five pixels and the grip was fifteen, so "past the fingers" was true only on paper.

The deep one is now placed past the fingers **and past the grip**.

And the other half: the server **refuses** a fragment pull with no forceps, before the
attempt is spent rather than after a minute of picking at a wound for nothing. An arrow is
still pulled by hand - a shaft with an end sticking out of somebody is exactly what fingers
are for.

## The eyes close

Minecraft has always counted how long since you slept and has always done one thing with
it: send birds. That is a threat from outside. What three days awake actually does is on
**the inside of your own eyelids**, and it is the one state in this mod that nobody but you
can see at all.

Past the first day - one night up is not a medical fact - the lids start coming down, and
they come down further the longer it goes on. But lids sitting at a height are a letterbox.
What actually reads as tired is the other half: **the blinks get longer and the gaps
between them get shorter.** That is what is drawn, with the edge of the lid soft, because
light does not stop at a line.

### And one of those blinks stops being a blink

This part is mine. At the far end of the count - rarely, and more often the further it has
gone - the eyes close and do not open for a second and a half. The world goes away and
comes back with you somewhere else.

Mechanically it is the shortest faint there is and it borrows that whole path: black
screen, nothing moves, nothing to treat and no time to. What it costs is a second and a
half of a moment **you did not choose**, and wherever you were standing. On a ledge, in
water, in front of something that was already swinging - that is the whole of it.

It never fires on somebody already on the floor or under anaesthetic; they have enough
happening. There is exactly one way out of the count, and it is the one the game has always
had.

# 1.8.0

Seven systems, and one of them is the first thing a body in this mod keeps.

## An artery

There were two kinds of bleeding and they were one kind at two speeds. The answer was
always the same answer; you simply had more or less time to give it.

An artery is **a different injury**. Not a worse cut: it is a body that is not losing
blood but **throwing it away**. It goes with the heart, it is brighter - that blood has
not been anywhere yet - and it does not clot, ever. A dressing over one soaks through and
keeps soaking.

The rate is **4.5% of volume per second** against one percent for a heavy bleed. Measured
on a live server: an artery in a thigh blacks the leg out in six seconds, has the pulse at
171 and thready by twelve, and puts you on the floor inside thirty.

There are exactly two answers, and both were already in the mod waiting for an injury that
truly needed them: **a hand pushed hard into it**, or **a strap above it**. While the hand
is there nothing comes out at all (measured: eight seconds at zero, and 4.5% a second again
the moment it lifts). A strap stops it dead, at the price a strap always charges.

You can tell it from an ordinary bleed **without opening a chart**, which matters more than
any of the rest:

- on the body it **pulses** - brightening and dimming on the beat, readable across a
  clearing;
- the particles are thrown sideways in bursts instead of dripping down;
- on the corner figure it flashes twice as fast, in a different red;
- it has its own chart icon: a jet going sideways rather than a drop going down;
- and the pressure game for it is **not the same game** - no pool with a run, but a jet
  that arrives on the beat, needing more weight and a longer hold, and lifting the hand
  does not cost you half the progress, it costs you all of it.

Rare on purpose: 12% of the wounds that would already have opened a heavy bleed, and only
in limbs and necks. The mod will not invent an artery in a belly, because there is no
answer to one - and that is not difficulty, it is a death sentence with extra steps.

## Going out

This is **not** the same as going down. Going down is a clock counting towards a grave.
A faint is a body deciding it cannot keep the head supplied **while standing up**, so it
stops standing up - which fixes it.

Seven seconds and you are fine. Nothing is damaged, nothing is permanent, nothing needs
treating. It is the mod's only free warning; everything else here tells you something is
wrong by taking something away for good.

What it costs is the seven seconds and **wherever you happened to be**. In a field it is an
inconvenience. On a ledge, in water, or in front of something that was already swinging,
it is the whole of the danger - and none of that had to be invented.

Three roads in: volume going (steeper the emptier you are), pain on its own with no blood
lost at all - the faint at the sight of a broken arm, usually somebody else's - and the
bill for adrenaline. Getting up and running multiplies the chance by two and a half.

**The screen goes completely black.** Not darker: black. You cannot see anything in a
faint, and the mod is not going to pretend you can nearly see.

## What it looks like from inside

The mod used to say all of this with vanilla potion effects. Losing blood handed you a
bottle of blindness; a rattled head handed you nausea. Those are blunt: tuned for potions,
fighting every other mod for the same effects, putting icons in the corner for things that
are not potions - and worst of all, **the same four effects for nine different states**. A
body running out of blood and a body with a fever looked identical from the inside.

Now the thing itself is drawn:

- **volume going** closes the edges in and takes the colour out - grey and narrow from the
  outside first, then a tunnel, then nothing;
- **the heart** shows in that narrowing: everything dims and returns **on your own real
  pulse**, so a screen that has started beating quickly is a reading nobody had to take;
- **a hit head** will not hold the horizon, and does not bring it back to quite the same
  place;
- **fever and sepsis** wash the room warm and soft;
- **cold** is the opposite: pale, hard, and a shiver instead of a drift;
- **a faint, and anaesthetic**, are black.

None of it changes anything about the body. It changes what the body can see. All of it has
a strength slider and an off switch in the client settings - the picture and the camera
movement separately, because it is the movement that makes people ill and nobody should
have to give up the rest to be rid of it.

## The splint stays on

A splint used to be a full stop: the bone was set, the item was spent, the stick vanished.

Now the stick **stays**. It is drawn on the body - two boards and two ties, over whatever
else is wrapped round the limb - it is on the chart, and while it is there the limb is
held: the bone is left alone whatever its owner is doing.

The clock on it is not a timer, it is a bargain. Walking runs it down. **Running puts time
back on**, every second of it, and it takes longer again the next time. That is the honest
version of what happens to a fracture nobody will stay off: you are not sent back to the
start, it simply takes longer.

Measured: a twenty second splint with sprinting on went 71 → 74 → 77 → 80 seconds, three
per second, and counted down again the moment the sprinting stopped. There is a ceiling, so
a bad week still ends.

When the clock runs out the splint comes off by itself and the bone is done - and at that
moment, and only that moment, the body writes down that it has broken there.

A splinted arm costs the quality of anything done with it; a splinted leg costs speed.
Neither is pain, so painkillers do not hide either: there is nothing to numb.

## Limbs come off blades now

The mod could take an arm off with a blast, and off with a saw. The gap between them was
exactly where it happens in life: **somebody with an axe**.

A blast is rare and a saw is a decision, so in eight versions nobody ever lost a limb to
the thing that actually takes limbs off.

It needs an edge. A zombie's fists do not do this, an arrow does not, a fall certainly does
not: it is a blade, swung hard, into a limb - mostly one already beaten past being a limb.
Through a sound one is possible and nearly impossible (4%), because that is how nearly
impossible it is. Set it to zero for a game where only ruined limbs come away.

What it leaves is the stump a blast leaves: open, unstrapped, bleeding heavily - and the
arm on the ground beside you with a clock on it.

## Food allergies

The mod's allergies were to three things in your own bag: antibiotics, morphine,
anaesthetic. Those are the allergies that matter to a medic, and they are **not the
allergies people have**.

The commonest in the world are foods. There are seven now - fish, wheat, egg, milk, honey,
mushrooms, berries - so seven of the ten possible are things you eat, and a spawn rolls
food more often than not.

Here is what makes it interesting: **a drug allergy is somebody else's problem** to
remember about you. A food allergy is yours, every time you open your own inventory. Bread
is what you eat when you are hurt, when you are bored, and when you are running away from
something, and having one item in that set which will put you down is the whole idea.

Nothing warns you and nothing refuses the click. You eat it, you gain the hunger, and then
your throat starts closing: anaphylaxis, the same one the wrong blood and the wrong drug
cause, and the answer is the same syringe the mod has always kept for it.

Your own is told to you in chat on the day it is rolled, and it still stamps onto a
bracelet - "fish" as readily as "morphine".

## The body remembers

This one is mine, and it is the largest thing here.

**Every injury in this mod healed back into the same body it started as.** Somebody who had
survived four amputations, a septic belly and a skull fracture was, once mended,
indistinguishable from somebody who had never been touched. A mod about medicine was
quietly saying that none of it mattered once it was over.

Parts remember now, and they remember three things:

**A knitted bone.** It is not weaker - and the mod will not teach that it is. But it is
where the next fall lands, because whatever broke that femur is the thing this body keeps
doing. And it **knows the weather**: an old break aches before rain, and aches indoors,
because what it answers to is the pressure rather than getting wet. Cold does it too. It
costs nothing, heals nothing and cannot be treated - the only thing in the mod that is
purely a person telling you about their body, and the only reason here to look at the sky.

**A scar.** Skin that has been opened and closed is not skin: less blood goes through it,
which is exactly why the next infection settles where the last one did - nearly twice as
likely on that part.

**Stiffness.** A deep burn closes by pulling its own edges together; a wound closes tight;
a limb sewn back on does not straighten the way it did. A stiff arm is not a weak arm - it
is one that does not go where it is put - so what it costs is the quality of work done with
it, and a stiff leg costs speed.

The counts **never go down**, and only dying clears them. The stiffness does, and that
split is the whole design: **the history ratchets and the disability does not.** A history
that only accumulates turns a long-lived character into one who is quietly getting worse,
and the fastest cure for that would be to go and die - which is the opposite of everything
this mod is for.

### Working it loose

A new thing to do with bare hands, and the only treatment in the mod aimed at something
**permanent**.

Take the joint out to the edge of what it will do and hold it there. Short of the edge
nothing loosens - being gentle with a contracture is how people keep one for life. Past the
edge something tears and it is tighter tomorrow than today.

The edge is **not drawn anywhere and cannot be**, because the only thing that knows where
it is, is the person whose arm it is. So they tell you: they tense before they say
anything, they say something before it hurts, and they shout before it tears. The whole
instrument is **the patient's face**, which is why it is drawn as large as it is - it is
not decoration, it is the readout.

Three sets. One session per limb per day, which is not a cooldown dressed up but the shape
of the treatment: what loosens a contracture is a short session repeated over weeks, not a
slider somebody can drag in a minute.

And on yourself it is worth half. You stop at your own pain. Every time.

## Bones move, and bones heal where they were left

For eight versions this mod had one kind of fracture and one answer to it: the bone broke,
a splint went on, the bone was set. That is the easy half of what a fracture is, and it
left out the half everybody who has seen one remembers: **the ends move.**

There are three rungs now, and each is a different job.

**Broken, in line.** Two pieces, and the pieces are where they were. A splint is the whole
answer, exactly as it always was.

**Out of line.** The limb is the wrong shape, and nobody needs a chart for that - everyone
in the clearing can see it. On the body it is a shoulder of bone standing where no bone
should be; on the chart it has its own icon; and on the splint screen the bone is drawn as
it actually is, two halves apart, instead of the one picture that used to be drawn over
every fracture whatever was underneath.

And this is what changes: **a splint stops being the answer.** Strap a stick to a bone that
is out of line and you have held it precisely where it should not be. The mod does not
refuse - "no time, no hands, splint it anyway" is a real decision and taking it away would
take away the rung - but it says out loud what will come of it.

**Bone through the skin.** The worst of the three and the only one that is also a wound: it
bleeds, it cannot close while there is bone in the way, and it is the fastest road to
infection here, by three times. No splint goes on it and nothing is set: a surgeon first,
to clean it out and close it. What is left afterwards is an ordinary displaced fracture.

### Setting it

A new thing to do with bare hands, three moves in order, and on screen it looks like
nothing else in the mod:

1. **Pull** along the bone and away from the body until the ends can pass. Not enough and
   they grind against each other - which is drawn. Too much and something between them
   tears.
2. **Line it up** while the pull is holding: the far half back under the near one.
3. **Let it down slowly.** The ends seat as the pull comes off, and a hand taken away all
   at once drops them together however they were lying.

Three outcomes rather than two: set; seated out of line (nothing spoiled, try again - much
the commonest first attempt); or torn, and in one case in five the end of the bone finds a
vessel, which is the artery from this same version.

**It is also the only treatment in the mod the room can hear.** A bone being pulled back
through a limb that can feel it is not quiet. Measured on a live server: a zombie with its
follow range zeroed - unable to find the player at all on its own - sat at seventeen blocks
and did not move, then closed to touching distance within six seconds of the scream. A
patient under the mask makes no noise, and so does one deep enough into the morphine, which
is the first reason to carry that mask that has nothing to do with quality.

### Healed crooked

A bone left out of line knits out of line - and it is **the only permanent loss of health
in the mod**: the part is worth less for good (measured: a leg at 14.0 becoming 12.3 the
moment the splint came off). It walks slower, and it aches on the weather louder than any
straight bone ever did.

There is one way out and it is the real one: **a surgeon breaks it again**, on purpose, so
that this time somebody can set it straight. It is the only treatment in this mod that is
the injury.

## The guide: thirteen sections

The guide had grown to a flat list of seventy-one pages, in the order they were written
over eight versions - which is the order they happened to the mod, and not any order a
reader has.

Now the list has a level above it: **one heading open at a time**. Somebody looking for what
to do about a burn wants the four burn pages, not the other sixty-seven.

| | |
|:---|:---|
| **Bleeding** | blood, **the artery**, the arrow, the strap, volume, the trail, reperfusion |
| **Bones** | a crack, a break, **out of line, setting it, bone through the skin**, **wearing a splint**, **healed crooked**, the skull |
| **Open wounds** | dressings, infection, a blacked part, the theatre, mending |
| **Burns and cold** | depth, pressing on one, smoke, going cold |
| **Chest and belly** | a crushed lung, organs, illness with a place, referred pain |
| **Illness** | the course, catching, viruses, worms, rabies |
| **Losing a limb** | dying, gone, **taken off by a blade**, sewn back, prosthetics, wear |
| **On the floor** | **going out**, the airway, arrest, the position, shock, carrying |
| **Readings** | pulse, breathing, temperature, thermometer, where it hurts, out of ten |
| **The bag** | painkillers, needles, the mask, allergy, taking things off |
| **The head** | concussion, deafness, the blast, adrenaline |
| **You, and the room** | **what you can see**, a second pair of hands, tired arms, sleep, cold, tags |
| **What it leaves** | **a scar, aching on the weather, stiffness, working it loose** |

Shut the open heading and the card holds thirteen rows and nothing else.

## Try it, over nobody

**Every procedure in this mod is learnt in the worst possible place.** The first saw a
player ever holds is over a friend who is losing the leg anyway. The first time anybody
meets the syringe game is the first time somebody's heart has stopped.

So a page with a procedure behind it now has a line at the bottom: **"Try it, over
nobody."** It opens the same game with **nothing on the other end of it** - no patient, no
packet, no item spent, no outcome. It cannot go well and it cannot go badly, and you can go
again immediately, which is the one thing a patient never offers.

It is the manikin in the corner of every teaching room, and it is here for the same reason
that one is: so the first time is not the time it counts.

Thirty-eight of the seventy-one pages have something to run. Nothing is unlocked by it and
nothing is refused without it, because a mod that made players practise before treating
would have invented homework.

# 1.7.0

This mod has been handing out burns since its first version and has never had them.

There was a flag. A part was burnt or it was not; a burnt part did not heal until somebody
rubbed a salve on it, and that was the whole of it. For an injury Minecraft deals out more
freely than any other - lava, fire, a campfire walked over, a blaze, a bed in the wrong
dimension - that is the thinnest thing in here, and it was thin in the way that matters:
**there were no decisions in it.**

Four facts turn it into an injury, and they are the four anybody who has dealt with one
would name.

## A burn has a depth, and depth is everything

Red heals itself. Blistered wants a salve. Through the skin will not close at all and no
salve will reach it. Nothing else about a burn decides as much - not where it is, not how
big it is, not what caused it.

| | |
|:---|:---|
| **Superficial** | red, painful, closes itself ninety seconds after it stops cooking. Nothing has to be done |
| **Partial thickness** | blistered, the most painful of the three. This is what a salve is for |
| **Full thickness** | through the skin. **Does not hurt**, will not close, a salve is no use |

## The worst one does not hurt

The nerve endings that would have done the hurting are part of what burnt.

So the patch somebody is screaming about is the one that is going to be fine, and the quiet
patch beside it is the one that needs a knife. **This mod has never had an injury that lies
by being painless**, and in life that is exactly what a burn does.

And it is not only in the text: **a deep burn charges you nothing in pain.** Superficial
costs 0.2, blistered 0.4, through the skin **nothing at all**. A player going by what hurts
will not see the worst burn on a body.

## It is not over when the fire is out

Heat sitting in tissue goes on cooking it for **about half a minute**. A burn that was
superficial when the flames went out can be through the skin a minute later.

That is the window, and it is the reason the first thing anybody is taught about burns is
water, and to keep it there.

- **Every burn arrives as the shallowest kind**, however hot the thing was. Depth is never
  decided on the tick it happened - because in life it is not, and because a thing decided
  instantly is a thing nobody can do anything about.
- **Water in that window** holds the depth where it is and closes the window twice as fast.
  Held under, a burn can **come back a step**. It is the only thing in the mod that undoes
  an injury rather than treating it, and the one place where that is true in life as well.
- **Snow and ice do the opposite.** A frozen burn goes deeper twice as readily: cold shuts
  down what little blood is still reaching it. It is the mistake every first aid course
  warns about.
- There is no half-measure: `cookSeconds` at zero and depth is decided instantly again.

## What kills is the size, not the depth

An arm burnt to the bone is a bad arm. **A third of somebody burnt at all is a person
emptying out through their own surface.**

The rule of nines, and it lands on these seven parts almost exactly - ninety-nine of a
hundred:

| | |
|:---|:---|
| head | 9 |
| each arm | 9 |
| chest, belly | 18 each |
| each leg | 18 |

Only what is burnt past the skin counts. A superficial burn is left out of this arithmetic
for the same reason it is left out of it in a hospital: it is painful and it is not a hole.

Past **15% of the surface**, volume starts going out through it, and from there everything
the mod already had does the work: the volume falls, the pulse climbs, shock arrives. **The
answer is saline, faster than it is going.** That is the third reason to reach for a bag
that is not a wound, and the most urgent of the three.

And one more: **skin is the thing that keeps heat in.** A big burn cools by itself and feeds
the same figure an operation feeds - so the circle the mod already has closes around burns
too: cold does not clot, and a big burn is a cold patient.

## The minigame: pressing on a burn

The third free thing whose reward is a fact rather than a repair. And the **only reading in
the mod whose finding is an absence.**

Press across the burn. Where it hurts, there is something living under it and it will close.
Where pressing does nothing at all, the burn is through the skin - and that patch is the
whole point, because it is the one a jar of salve is wasted on.

- **Press in one place** and you learn that it is burnt, which you could see.
- **Press across the whole of it** and the mod names the depth and tells you which of the
  two answers it wants: the jar, or the knife.
- Works on somebody standing up, works on yourself, costs nothing and cannot be failed.
  Finding a patch of your own that feels nothing is the worst thing you will learn today.

## What answers one

**Depth is a ladder and every rung has its own answer.** A jar does not erase a burn; it
takes it down a rung.

| | | |
|:---|:---|:---|
| full thickness | the **knife** takes the dead skin off | → partial thickness |
| partial thickness | the **salve** | → superficial |
| superficial | nothing | closes itself in ninety seconds |

- **The salve is exactly the middle rung.** On a full thickness burn it does nothing: a
  salve keeps living tissue from drying out while it closes, and under one there is nothing
  living left. And on skin that is only red it is a jar spent on a burn that was going to
  close anyway - so the mod refuses it and says **keep the jar**.
- **The surgical kit** - full thickness. What is dead has to **come off**, and it is the one
  wound in the mod answered by taking something away rather than putting something on.
- **Nothing treats a superficial burn.** It never used to close at all: the flag turned every
  brush with a campfire into an errand for a jar.

So a bad burn costs a knife, a jar and ninety seconds, and a mild one costs only time. A jar
that erased the whole burn skipped two rungs of three and made the depth decorative at the
moment it matters most.

Checked on the live server: 3 to 2 to 1 to clear on the ninetieth second - and at full
thickness the arm **says nothing**, while at partial it starts to ache.

## The half of fire that is breathed

Everything on the outside of somebody is measured in hours. **An airway is measured in
minutes**, and it happens to people who walked out of it looking fine.

It is caught in fire or lava, and there is nothing on the skin at all. The tell is **the
voice going**, and the breathing turning fast and shallow: +12 to the rate and "shallow" on
the chart.

**No bottle touches it**: this is swelling, not infection. And two things make it lethal,
both of which the mod already had:

- **Morphine.** It presses on the drive to breathe, and the mod already knew how to punish
  morphine on a chest that is failing. An airway that has breathed fire is such a chest.
  Give morphine to somebody who breathed fire and they can stop breathing altogether.
- **A face burnt through.** Together with the smoke, that is what closes an airway - and a
  face burnt through is the oldest warning sign in the trade, which means the swelling can
  be **seen coming** rather than arriving from nowhere. Neither one alone does it.

What answers it is a pair of hands: anybody standing there can hold the airway open, over
and over, for as long as it takes. There is no item for this and there will not be - the
item would be a tube down the throat, and that is further than this mod goes.

## The thermometer minigame is rebuilt

It was a bar that filled while the button was held. The note above it argued that waiting
**was** the content, because a temperature cannot be hurried. That is true of the reading and
not of the instrument, and it left a game with nothing in it: press, wait, let go.

A mercury thermometer asks three separate things of a pair of hands, and every one of them is
something anybody who has held one remembers doing.

### Shake it down

Mercury does not go back on its own. That is the whole trick of the instrument - it is why
one can be taken out and read at leisure - and it is why it opens **still showing the last
person's fever**. A thermometer that is not shaken down does not read the patient in front of
you.

It is the same idea as the grime on a saw: the instrument carries the last patient into this
one, nobody is told, and the fix is free and takes a second. Hold and swing - the column drops
in steps, and you can see how much is left in it.

**Not shaken means a band rather than a figure**, and the mod says it was the last reading.

### Hold it in the fold

The column climbs fast, then slower, then hardly at all - the shape mercury has.

But a body is not a table: it breathes, and the fold moves with it. Out of the fold **nothing
climbs**. Nothing falls either, because mercury does not fall. The cost of not paying
attention is time, and time is what this reading is made of.

### Read the thread

The column is a hair of metal behind a lens of glass, and from almost every angle it is
**invisible**. You turn it in the light until the thread flashes against the scale, and only
then can you say the number. That is the part everybody who has held one remembers, and it is
the part the old game left out entirely.

It still cannot be failed. Taken out early the figure is low; unshaken the figure is somebody
else's; both come back as a band rather than a number, which is what an unreliable reading is
worth.

## What you can see

- **Three burns on the chart instead of one.** They are three different problems - one wants
  nothing, one wants a jar, one wants a knife - and a single "burnt" icon hid the only part
  that decides anything. The deep one is the darkest mark on the chart, because that is what
  one looks like.
- **`/triage info` adds up the surface**: "Burnt: 36% of the surface, worst full thickness".
  That is the figure that decides what happens to a burnt person, and it is the sum a medic
  does in the first ten seconds.
- `/triage burn <part> <0-3>` instead of the old `true/false`.
- **The three burns on the chart are drawn as three different things.** I first told them
  apart by colour alone - two shades of orange eight pixels across. Depth decides what a burn
  wants doing about it, and that is the last thing to hang on the channel people compare by
  looking twice. It is a small flame, a flame with blisters beside it, and **a dark patch
  with a hard edge** - not a flame at all, because that is what one through the skin looks
  like.
- **The thermometer and the three syringes are redrawn.** The thermometer was a dashed
  diagonal with a red blob: a stick with a red tip. The syringes read as small bottles - no
  plunger, no needle. The thermometer now stands upright with its scale and its bulb, the
  same instrument the minigame draws, and a syringe has both of the ends it is recognised
  by: the thumb rest and the needle.

## The body shows more

**A bandage was a painted cube.** A box of white wool filled with a flat colour - and it is
the commonest treatment in the mod.

Now it is **turns**: overlapping edges at an angle, each a shade off its neighbour. And what
has come through one is not a colour change, it is a **blot** - one thing, in one place,
where the wound is. A soaked bandage has its own skin rather than the same skin in a browner
tint.

And two things that could not be seen on a body at all:

- **Burns.** The mod spent a whole version making them real and drew them nowhere. A burn is
  on the outside of somebody by definition, it has a colour, and the difference between the
  depths is a difference in **how it looks**. Red, blistered and through are three different
  patches on the skin.
- **A limb that has died on the body.** The mark always said there or gone and never said the
  state between them - and that is the one with the decision in it. A limb that is gone asks
  nothing of anybody; one that is dying asks for a saw today. The strap that killed it has
  been visible for versions; now so is what it did.

All of it fits in the same `long` the marks travel in: forty-four of sixty-four bits were
spoken for.

## An allergy that arrives with the person

Rolled with the blood type and for the same reason: it is a fact about somebody rather than
an event in their day, and it is wanted **before** a bag is opened over them.

It is one of three things in your own bag - **the antibiotics, the morphine or the
anaesthetic**. Give it to somebody who has it and they go into anaphylaxis, which this mod
has always had and has always answered with the needle it keeps for exactly that. Nothing new
had to be built: what was missing was the commonest cause of it in the world, which is not a
transfusion but a drug given to somebody who would have warned you.

**The dose is spent.** It is not a refusal - it is a treatment that went in and did the wrong
thing.

And here is why it is interesting: **you can ask.** A conscious patient tells you what they
cannot have, in the same breath as where it hurts. A patient on the floor tells you nothing -
so the same dose is a treatment on a man who can talk and a killing on the same man ten
minutes later. Nothing else in this mod turns on whether the patient can still answer.

## The bracelet is the part I added myself

The allergy leaves a hole, and it is a real one: an unconscious patient cannot warn anybody.

That hole has a real answer and it did not have to be invented. People who cannot afford that
conversation to fail **wear the sentence on their wrist**. It is the oldest piece of medical
equipment that exists purely for communication, and it exists because of exactly the gap this
mod now has.

Stamp one with your own - only your own, the item knows what the body knows - and anybody who
looks at you reads it, talking or not. It treats nothing, protects against nothing, and costs
two nuggets. What it is worth is decided entirely by whether somebody thinks to look before
they open the bag.

## The arm lands on the floor

Losing a limb here was final in the flattest possible way: the arm stopped existing the
moment the saw finished. That is true of an amputation somebody chose. It is not true of the
other way limbs come off - **something takes one off you, and the thing it took is lying four
feet away.**

So it drops. It is an item: it can be picked up, carried, handed over, dropped down a hole
and lost, and for a few minutes a surgeon can put it back.

What decides that is **not the surgery**. It is two things that happened before anybody
opened a screen:

| | |
|:---|:---|
| warm in a pocket | 5 minutes |
| **in water or snow** | three times as long |

That is the real advice - a severed hand travels to hospital on ice - and it is the one thing
about this everybody has heard and nobody expects a game to know.

It comes back **worse than it was**: weak, stiff like a set bone, and dirty, because it has
been on the floor. And everybody who has the choice still takes it over a hook.

**None of it makes anything survivable that was not.** The stump bleeds the same, the shock
arrives the same, and somebody still has to close it. What it adds is a second clock running
beside the first and a decision about which to spend the minute on.

A limb that was already dead when it came off is meat. The item still drops and still says
so - a puff of smoke would teach nothing, and somebody carrying a dead arm around knows
exactly what they are carrying.

## Worms

The commonest illness in the world and the one nobody puts in a game, because there is no
scene in it. It does not burn, it opens nothing and it will not kill anybody this afternoon.

It **takes**: it eats before you do and drinks off the blood while it is there. What it
leaves is somebody hungry, short and tired for weeks with no dramatic moment in the whole
business.

Which is exactly why it belongs here: **nothing else in this mod is slow.**

It does not pass on its own - nothing about a parasite is self-limiting - and **a bottle of
antibiotics does nothing whatever for it**. That is the second thing on the far side of the
line the mod has spent two versions drawing. But unlike a virus it has an answer of its own,
and that answer is **a second card of tablets** which answers nothing else.

Two cards that look alike, do different jobs and refuse each other's work is the clearest way
this mod has of saying that medicine is specific.

## People say things now

Everything in this mod has been told in the voice of a chart: a bone breaks and a line names
the bone. That is the right voice for a readout and the wrong one for the moment itself,
because the moment is not information. **It is a person making a noise.**

Now the worst of it comes with something said out loud, and whoever is standing near hears it
(fourteen blocks). Seven occasions, twenty-nine variations: a break, a burn, an illness, a
limb, a reaction to a drug, heavy bleeding, something left in them.

> *something went* · *water, water* · *I told you, I told you* · *pick it up, pick it up*

Variations rather than one line apiece: the second time you hear the same six words the mod
stops having a voice and starts having a string. The same line never comes twice running.

**None of it is a mechanic.** No line does anything. The point is that a mod about bodies
with nothing to say about pain was describing bodies rather than people. As a side effect it
is also a medic's first warning, and the only thing a bystander gets without walking over and
opening a chart.

## Its own minigame for each new item

- **The graft.** The only treatment here that joins two pieces rather than repairing one,
  and that is different work. What decides it is not how neatly the skin was closed but
  **whether the bone lined up** before the first stitch: turn the end until marrow sits
  against marrow, let go, then go round the rim one stitch at a time, each where the last
  one left off. The grade is almost all bone - every stitch has to go in anyway, and the
  bone cannot be gone back over.
- **The worming tablets.** Not the blister game: foil presses the same either way and there
  will not be a second game about foil. This is the other half - **it is the tablet that
  will not go down a patient ill enough to need it.** Crush it under a spoon until there
  are no lumps, then stir it into water until nothing is sitting on the bottom. Half
  crushed leaves grit, grit settles, and what settles is the dose nobody got.

## Fixed in the shrapnel game

Two bugs, and they are the same bug twice: **the fragments were placed in one geometry and
reached for in another.**

They were scattered by spread-across and depth-down. The forceps pivot about the mouth of
the wound, which means they reach in a **radius** - so on a wide panel a fragment out to the
side sat inside the rectangle and outside the circle. No angle would ever touch it.

The same mismatch caused the second one: **bare hands could finish the whole job.** Fingers
were given a shorter radius while the fragments were scattered by depth, so most rolls put
every one of them inside it. "Fingers reach what is near the surface and nothing else" was
not a rule, it was a coin toss that mostly came up fingers.

Now they are placed the way they are reached for: an angle below the skin and a distance
from the mouth. And **the deepest is always further than a finger goes** - bare hands can
pick at the edges and never finish, which is exactly what the jaws are for.

Checked by enumeration: eight thousand rolls across four panel sizes - **no fragment out of
reach of the forceps, and no roll finishable with fingers alone.**

## Out of ten

The first question anybody in pain is asked, anywhere in the world. This mod had the
**second** one - where it hurts - and not this one at all.

It is a **third kind of reading**, so there is now one of each:

| | |
|:---|:---|
| a pulse | a fact about a body, taken off it with two fingers |
| a temperature | a figure from an instrument, and it costs the instrument |
| **a score** | an **opinion**, out of somebody's mouth, and nothing can check it |

So it is wrong in more ways than anything else here - and every one of those ways was
already built:

- **Pills and morphine** take it down without touching what is wrong.
- **Adrenaline** takes it down and sends the bill twenty seconds later.
- **A hit head** does not rank things, and neither does a mind that is somewhere else: both
  answer with a number that wanders.
- **Shock makes people quiet.** The quietest casualty in the clearing is often the worst
  one, and that is not stoicism, it is a body shutting down.
- **A burn through the skin says nothing**, because the nerves that would have reported it
  are part of what burnt.

**Which is why you ask.** A two on a heart running at a hundred and thirty is not a
comfortable patient - it is one somebody **has already dosed**, and the mod says so to
whoever asked. Four seconds of asking is cheaper than finding out with a second dose.

There is no minigame, for the same reason asking where it hurts has none: those two are the
only things in this mod that are a conversation rather than a procedure.

Checked on the live server: whole - says nothing; one broken leg - 5 of 10; plus an arm and
a bleed - 6, adding with diminishing returns; after painkillers - 1. **A burn through the
skin - nothing. The same burn plus a blistered one - 3.** And a quiet patient at a pulse of
153, which is the pair the warning fires on.

## Compatibility

Drops onto 1.6.0. A body saved before this version reads as it is: where the flag was, there
is now a **partial thickness burn** - blistered, painful, answered by a salve, which is
exactly what that flag always described. Reading it as anything else would either invent
injuries or erase them.

## Config

| | |
|:---|:---|
| `burns.enabled` | off - fire is just damage again |
| `burns.chancePerDamage` | 0.08 per point of fire damage |
| `burns.cookSeconds` | 30 - how long a burn goes on deciding how deep it is |
| `burns.deepenPerSecond` | 0.05 a second inside that window |
| `burns.coolBackChance` | 0.5 - chance one held under water comes back a step |
| `burns.iceFactor` | 2.0 - how much readier a frozen burn is to go deeper |
| `burns.fadeSeconds` | 90 - how long a superficial burn takes to close |
| `burns.fluidFloor` | 0.15 - the share of surface at which volume starts going |
| `burns.fluidDrainPerSecond` | 0.02 volume a second, per share over that floor |
| `burns.chillPerSecond` | 0.004 - how fast a burnt body cools |
| `burns.smokeChancePerSecond` | 0.08 a second in fire; 0 turns it off |
| `burns.smokeSeconds` | 240 |
| `burns.smokeBreath` | 0.35 |

# 1.6.0

A treatment here has always been worth the same wherever it happened and whatever it was
done with. A kit opened in a lit room over a table did the same job as one opened face-down
in a cave, with the same saw that took somebody else's leg off an hour ago and has not been
touched since.

Both halves of that are wrong, and the second is the famous one. For most of the history of
surgery the instrument was the thing that killed people - not the cutting, the carrying: a
blade went from one patient into the next and nobody thought anything of it.

And one more. Everything in this mod ended at the moment the treatment did. A surgeon closed
somebody up and they stood straight back up and went to work, which is the one thing about
surgery that nobody who has had any believes.

None of it punishes anybody for playing the way they already do. **A clean instrument in an
ordinary lit room is worth exactly what a treatment has always been worth** - that is the
middle everything else is measured from. What opens up is a direction in each sense:
somewhere good is better than the old baseline, and a filthy saw in the dark is worse.

And a fourth thing joins those three: **the surgeon**. The arms that carried somebody
across half the map, and the arms of somebody three days awake, are not the arms to be cutting
anyone open with, and nothing but the mod is going to mention it.

And two more that were missing before a temperature could mean anything: **the instrument**
it is taken with, and **the illnesses a bottle does not answer**. Neither makes sense on its
own - together they are a forehead you have to decide about, with a course in your hand.

Drops onto 1.5.0. Old worlds and old bodies load as they are.

## Where you operate

Three things, and they are the three anybody would name: is there somewhere to put the
patient, can the person working see, and is the place shut off from the weather and
everything living in it.

| | |
|:---|:---|
| A **medical table** within four blocks | 0.5 |
| **Light** - from a lamp, not from the sky | 0.3 |
| A **roof** - anything over their head | 0.2 |

The table is worth most because it is the one of the three you have to decide to build. The
other two are nearly free indoors, which is the point: a room is already most of what an
operating theatre is.

The light counted is block light. A surgeon who can only operate at noon is not a surgeon,
they are a sundial. Nothing counts below level 4, everything counts from 12.

| place | quality | infection with a clean tool |
|:---|:---|:---|
| open sky, in the dark | x0.75 | 18% |
| a cave: roof yes, light no | x0.85 | 11% |
| **an ordinary lit room** | **x1.00** | **0%** |
| that room with a table | x1.25 | 0% |

The multiplier lands on how well the minigame went, before anything else - so the place
moves the result of the operation and everything downstream of it.

## What you operate with

An instrument that has been through somebody is dirty and stays dirty.

Only the things that are carried rather than spent, and only the ones that go into a wound:
the saw and the forceps. A bandage that has been used is gone, so it cannot be used again
dirty, and **bandage scissors are left out of it** - nothing here has them opening a wound,
so dirt on them would be a chore with nothing at the end of it. The one thing this must not
turn into is a durability bar on a consumable.

Six patients and an instrument is as dirty as it gets: **55%** that the treatment leaves
something behind in the wound. One patient is 9%, three is 27%.

The instrument and the room add together, and the instrument is the bigger half
deliberately: a bad room is bad luck, and a filthy blade is a decision somebody made and
kept making.

## What cleans it

The **medical table** has been decoration with a job title: it told a village where to put a
medic and did nothing else. A strange thing for the one block in a mod about medicine to be.

- **The table.** Right-click it with an instrument and the instrument comes back clean.
  Free, as often as you like, and it does not travel.
- **Antiseptic.** Cleans the **whole bag** at once, in the field, and costs the bottle.
- **Water.** Drop a dirty instrument in water and it comes off - one patient per six
  seconds - but not all of it: the last of it still wants a table or a bottle. Water
  **rinses, it does not sterilise**, and it never has: what comes off a blade in a stream is
  the blood, which is the part you can see and the part that matters least, and the stream
  makes its own contribution. Which is the right shape for a field expedient: better than
  carrying a filthy saw, and worse than doing the job properly.

That is the trade: the table is free and stays where it is, the bottle travels and is gone,
and water is free everywhere and does not finish the job.

## A second pair of hands

The third axis beside where and with what: **who**. Everything here is easier with somebody
else, and for a long time somebody else could do everything except the one thing an
assistant actually does, which is hold on.

**Crouch next to the patient with an empty hand and you are holding them.** Both are
deliberate - nobody does either by walking past - so the mod never has to guess whether you
meant it. One person, not a crowd: a second assistant is not better surgery, it is a room
full of people.

- **+0.15 on how well the work goes**, wherever you are standing. A pair of hands on a cave
  floor is worth what a pair of hands is worth.
- **The ceiling on operating awake rises from 0.5 to 0.7.** Somebody being operated on
  without anaesthetic moves, and held down they move less - which is what an operation was
  before anaesthetic and why every picture of an old operating table has straps on it. It
  does not stop the pain and was never meant to. It is still worse than the bottle.
- Both people are told, because otherwise nobody would ever find out: an assistant
  crouching with an empty hand looks exactly like an assistant crouching.

## The three needles

Everything else in the bag is something you do with your hands over a minute. These are
decisions made in a second, and every one of them can be the wrong one.

### Adrenaline

Two right uses, and both are real.

- **In a stopped heart** it restarts nothing - nothing in a needle restarts a heart - it
  gives the next round of compressions back its full value, which is exactly what it is
  given between cycles for.
- **In a reaction to blood** it is the only answer there is.

**Its wrong use looks exactly like its right one**, which is what makes it the most
dangerous thing here. Somebody emptying out looks like somebody fading, and the instinct is
to push something into them. Adrenaline raises the pressure: every light bleed turns heavy
and the volume goes while you are still feeling useful. **A second shot on top of the first
is the other way a heart stops** - and injecting during the crash counts as the same thing.

### Morphine

The opposite of the pills: at once, strong, works on somebody already on the floor, and it
does not care how many courses they have had because it is not the same drug.

What it costs is **breathing**. That is not a balance decision, it is what an opiate does:
the drive to breathe is what it puts to sleep first. On a chest that is already failing -
crushed, broken, full of something - it can stop them breathing altogether. They go down,
the airway clock starts, and a pair of hands answers it.

### Naloxone

Undoes morphine and nothing else. On somebody whose breathing has gone it is the difference
between a patient and a body.

Its own wrong use is why it is a decision and not a reflex: pushed into somebody who was
being held together by the dose it does what it says on the ampoule. It takes the morphine
away. **All of it, at once** - and what arrives instead is every bit of what was being kept
off them.

## The minigame: the vein

Anybody can push a plunger, so the game is not about that. The difficulty of a syringe is
getting into a vein and **knowing that you have**.

One thing tells you: **blood in the hub**. It appears the instant the tip is in the lumen,
and it is the only confirmation that exists. Inject with it and the dose is in the
circulation. Inject without it and the dose is in the muscle of somebody's arm, where it
does a fraction of what was wanted and does it late.

The first version of this was a schematic: bars, an arrow, and a caption saying what they
were. The game worked and nothing in it was recognisable as a syringe.

Now it is a forearm and a syringe you put into it, in three movements anybody would know:

- **Aim** the needle where you think the vein is.
- **Hold the left button and it goes in.** At an angle, because nobody goes into a vein
  perpendicular, and the steel greys out behind the skin: you can see exactly what you can
  see in life, which is how much is still outside.
- **Let go when blood appears in the hub.** A needle that kept going came out the far side,
  and the hub is empty.
- **Hold again and it is the plunger now.** Fluid runs while you hold and stops when you let
  go, and all of it has to arrive: the quality is the share of the dose that went in with
  blood in the hub, not the fact that the button was pressed.

The needle **drifts** while all this is happening, because a hand is not a tripod, and
keeping it on the vein is work for the mouse. Drawing back is free and unlimited - nobody is
punished for withdrawing a needle. The ampoule does not come back, a pushed syringe is an
empty one, and there is no failure in the game, only a number.

**And the difficulty comes from the patient, not from a setting.** A vein is held open by
what is inside it, so somebody who has lost volume, gone into shock or got cold has veins
that are flat and barely visible - which means the needle is hardest to place in exactly the
person who needs it most, for the same reason it is in life. You can see it: the worse they
are, the thinner and fainter the vein is drawn.

How well it went scales everything, the harm included. A dose that did not arrive cannot do
damage either.

### Recipes

| | |
|:---|:---|
| **Adrenaline** x2 | bottle + blaze powder + sugar + iron nugget |
| **Morphine** x2 | bottle + 2 poppies + iron nugget |
| **Naloxone** x2 | bottle + fermented spider eye + sugar + iron nugget |

Poppies because that is where morphine comes from. The fermented spider eye because it is
the ingredient vanilla itself uses to turn a potion into its opposite.

## The fragments: not a rhythm, a channel

That game was a wound in section with a piece of timing laid on top of it: hold, and let go
inside a window nobody could see. That is a rhythm game wearing a cross-section, and none of
it was the job.

The job is geometry, and it is the same geometry every time. A fragment **tore a channel** on
the way in, and that channel is the only route back out that does not cut anything new.

- **The forceps go in where the hole already is.** An instrument cannot pass through skin
  that is not open, so here they **pivot about the mouth of the wound** and everything else
  is angle and depth.
- **Then you follow the track down.** It is drawn because it is the task.
- **Leave the track and you are not removing a fragment, you are making a second wound with
  a pair of forceps.** It is visible while it happens, and past enough of it the piece is
  marked: it came out of a wound the rescue made.
- **Fingers do the same job worse.** They reach what is near the surface and nothing else,
  they take longer to get hold of anything, and they have less room to be off by. That is
  the difference a pair of jaws buys, and it is the answer to why the item exists.

## The pulse: two fingers that have to press

The arm was two rectangles, and two rectangles are not an arm. Now it is a forearm, a wrist
with its creases - which is what anybody actually finds the thing by - a palm and fingers,
and **its own pair of fingers doing the feeling**: the skin gives under them the harder you
press.

And the question became two. The game used to ask one thing, whether the fingers were in the
right place; **a wrist also asks whether they mean it.** Resting fingers on somebody does not
find an artery. A weak pulse has to be **pressed for**, which is why a thin pulse is the hard
one to count here, for the reason it is the hard one to count in life.

**You can no longer press too hard.** There was a ceiling - bear down and the vessel shuts,
which is true of a real wrist - and it came out. Not because it was wrong, but because it was
not worth what it cost: the number you are after is the same either way, and all it added was
a second invisible thing to get wrong while counting.

## Where it hurts is not where it is

A ruptured spleen hurts at **the tip of the left shoulder**. It is called Kehr's sign, it
is real, and it is how that injury gets found by somebody with no machine to hand. A torn
liver does the same on the right. A heart goes down the left arm.

So asking where it hurts can now send you to the wrong place - and the patient is **not
lying and not confused**. They genuinely feel it there. The mod does not mark the answer
as suspect, because nothing about the patient is suspect.

**Everything else gives it away.** The chart says the shoulder is untouched. The complaint
says the shoulder is the worst thing in the body. Those two cannot both be about a
shoulder. Add a pulse running hard with nothing open on them, and there is one thing left
it can be: the belly is filling up.

The only tell in the words themselves is that they say **the top of** the arm, not the arm.

**And it is fair.** It only ever fires when the named place is genuinely unhurt. If the
shoulder were broken too they would have been complaining about the shoulder anyway, and an
answer that could mean either is not a clue, it is noise.

This is the only thing in the mod that is wrong on purpose, and it is the most useful
sentence a patient can say.

Verified on a live server: spleen torn, left arm **12.0/12.0 and untouched**, the body says
"the top of their Left arm hurts", pulse 100. Break that arm for real and the referral
switches off - it names the stomach.

The complaint also got a line in `/triage info`: by this command's own rule, anything not
drawn on the figure has to be readable there or nothing can check it.

## The circle this mod already had two thirds of

Two sides of the oldest idea in trauma surgery have been in here for a long time, unsaid.

**Cold blood does not clot** - somebody with hypothermia never stops bleeding on their own
at all: a light bleed that would have closed by itself runs until a dressing goes on it.
And **losing blood is itself cooling** - the temperature drops as the volume does.

The missing third is the one that closes the circle: **an open body loses heat.**

Until now the only way to get cold was to stand in powder snow or in the rain in the wrong
biome. A man lying opened up on a table in a blizzard caught nothing at all. In life he is
the likeliest person in the world to, which is why an operating theatre is kept
uncomfortably warm for everybody standing up in it, and why the first virtue of a trauma
surgeon is speed.

**Where you cut decides the price.** A lit room out of the weather is nearly free. The
open, in the snow, in the rain, at night, is not - and enough of it is hypothermia, after
which nothing they have closes by itself any more.

- Scored by place: a roof -0.35, firelight -0.35, a cold biome +0.4, rain +0.3.
- Ninety seconds to warm back up, **twice as fast by a fire** and half as fast in the cold.
- It reads on the forehead: up to -2.5 C off the number a medic already takes.
- Both people are told, and the one holding the knife first: the patient is usually on the
  floor and cannot carry themselves to a fire.

Nothing new punishes anybody here. Every consequence was already written and already
right - only the cause was missing. There is no item for it and there is not going to be
one: somewhere warm, a fire, a roof, and be quick.

## A sneeze is not addressed to you

A sneeze here was a sound and a line of text addressed to the person sneezing, which is
the one audience a sneeze is not for. The `body` parameter was not even used.

It now does the three things a sneeze does:

- **It is loud.**
- **It is catching.** Anybody within four blocks can pick the cold up off you - **the
  first thing in this mod that travels from one player to another.** A cold and not
  anything worse, on purpose: an illness that spread and also mattered would make standing
  near a wounded friend the wrong move, and this whole mod is built on that.
- **It hurts.** With a broken rib, separately so. And if your chest or belly is still
  knitting from an operation, a sneeze can open it - the only way here to come apart
  without doing anything at all.

## Illness with a place

Two illnesses that happen to an organ rather than to a person. Both are a missing middle.

### Pneumonia

A cold used to sit at mild-and-passes however long somebody spent out in the rain. After
about three minutes it can now **go down into the chest** - but only while they are still
wet or still cold. Getting dry and warm is the whole of the answer.

It **bruises both lungs** rather than tearing anything, and that is the honest difference
between an illness and a wound: there is no hole for a surgeon to find, so opening the
chest achieves nothing. It reads on the breathing line and on the forehead, and only a
course answers it.

### Peritonitis

A torn gut used to roll straight for sepsis - from the wound to the worst thing in the mod
in one step, with nothing in between to notice or act on. There is a step now: it says so,
runs hot, and eats the belly.

And it is **the one illness antibiotics cannot finish.** While the gut is still open the
cause is still arriving: a course puts the clock back to the start and buys time. The
bottle does not end this. The surgeon does.

Verified on a live server: pneumonia bruised both lungs, breathing 14 to 23 and laboured,
temperature 37.0 to 38.4. Peritonitis reads 38.6 exactly and turns to sepsis at 39.0 on its
own clock. A second player four blocks from a sneezing one catches the cold; sixty blocks
away they do not.

## The thermometer

Every other reading here is taken with the hands and is there the moment you look. A
temperature is not. Anybody can feel that a forehead is hot; **thirty-eight point six** is
not something any hand has ever reported, and the mod said it anyway.

So **the word stays free**: the chart and `/triage info` still say hot, cold, normal. **The
figure now costs an instrument** - and the half minute the column takes to settle. Which is
the right price, because the figure is what separates a fever starting from one that is
already over.

**A minigame whose content is waiting.** The column climbs fast, then slower, then hardly at
all - the shape mercury actually has, and the reason the last tenth of a degree takes longer
than the first two. The whole skill is knowing it has stopped.

**It cannot be failed.** Pull it early and you get a number, the number is too low, and
nothing about it looks wrong - exactly the mistake the instrument invites in life. So a
thermometer taken out early hands over a band rather than a figure.

| | |
|:---|:---|
| **Thermometer** | 2 glass panes + redstone + iron nugget |

Not consumed.

## Viruses

**A course of antibiotics does nothing to a virus.** Not less, not slower - nothing at all,
except that the course is gone. Everything the bottle answered in this mod was bacterial, and
there was never any need to say so until there was something else.

### Influenza

Runs hot, spreads off a sneeze the way a cold does, and **passes on its own** given time and
somewhere warm. Most of the time that is all it does.

It comes from where influenza actually comes from, which is **a crowd**. This game has one,
and it is a village: stand about in one long enough and somebody in it was ill. Nothing is
tracked or stored about the villagers themselves - it is simply that standing among a lot of
bodies for a long time is how this is caught, and a village is the only place in Minecraft
where that is true. It is also what makes the sleep rule mean anything to somebody playing
alone: not sleeping has always multiplied the chance of catching what is going around, and
until now nothing was going around.

On the way it can **leave the chest open to pneumonia** - which is how influenza has always
killed people, and which is the one part of it a bottle does answer. So the course is not
useless: it is for the complication, not for the virus.

And that "can" is decided by the body rather than by a clock. Cold, wet or three days awake
and the chest is likelier to open, and likelier the longer they have been ill; dry, warm and
rested and it **will not open at all**.

### The stomach one

Comes off what was eaten: some of what used to poison people here is not poison now, it is
alive. It does not burn, it **empties** - it pulls volume. It has no cure and does not need
one: what it needs is fluid, faster than it is going out. That is the first reason to reach
for a bag of saline that is not a wound.

### And the forehead is how you tell

A virus and an infected wound **both run hot, and only one of them is worth a bottle.** Which
is why the thermometer and the viruses arrived in the same version: the instrument became
what it is for.

Try to cure a virus with a course and the bottle is **not spent**, and the mod says why.

## Not feeling it yet

A wound here used to cost you from the tick it happened: a leg breaks and the limp starts
on the same tick, which is the one part of breaking a leg that nobody who has broken one
recognises.

A bone going, or a limb going out, now buys about **twenty-five seconds** in which most of
the pain is not there. People finish races on stress fractures and walk away from crashes
on broken ankles.

**It hides pain. It does not mend bone.** A part that is gone, dead on the body, or beaten
past working at all is not helped **at all** - what is missing there is not the will to
ignore it, it is the thing that carries the weight. A wooden leg stays exactly as good as
it was: that limit is mechanical, not painful.

And it is not a gift:

- **Your heart is already going** - +30 on the pulse, and the only sign of it anybody else
  can read. Somebody standing on a broken leg saying they are fine, with a pulse of a
  hundred and twenty, is not fine.
- **Your hands shake** - -0.15 on anything you treat during it. The minute in which you
  cannot feel your injuries is the worst minute to operate on anybody.
- **Then it arrives.** Twenty seconds in which everything is a third worse than it would
  have been if the surge had never happened.

There is no way to ask for it, and no second one until the first has been paid for.

Measured on a live server: a broken arm costs -0.150 attack speed, -0.030 on adrenaline,
-0.195 through the crash. A blacked-out leg stayed at the full penalty throughout, as it
should.

## Arms give out

Two things here are physically heavy and this mod invented both of them: carrying somebody
who cannot walk, and bracing somebody who is being cut into. A medic used to be able to do
either forever, which made getting them out first a formality rather than a decision.

**It is not a stamina bar.** Nothing you normally do builds it - not sprinting, not mining,
not fighting. Vanilla already charges for those, and charging twice would be a tax on
playing rather than a rule about medicine.

- **Carrying drains it for as long as you keep walking.** Ninety seconds with sound arms.
- **Holding a patient still costs a lump**, 0.15, paid when the treatment lands. Which is
  why an assistant cannot be one indefinitely - otherwise the answer to every operation
  would be "have a friend crouch here for the rest of the session".
- **A carried body gets heavier as you tire**, and at the end your arms give out and put
  them down wherever you happen to be standing. Not damage - just the wrong place.
- **A broken, dead or missing arm tires up to three times faster.** That is what a wounded
  stretcher-bearer is, without needing a rule of its own.
- **Tired arms are not steady ones**: up to -0.25 on how a treatment goes. The fourth thing
  that decides an operation after the room, the instrument and the second pair of hands -
  and the first of two about the surgeon rather than the arrangements. The other one is
  below, and it is about sleep.

It comes back over 45 seconds, **twice as fast standing still**. That is the second rule
here whose answer is to do nothing, and the two of them are what this mod is proudest of.

The `/triage info` line only appears once there is something to say. Fresh arms say nothing.

## If you do not sleep

Minecraft has counted this from the beginning and has always done exactly one thing with it,
which is to send birds. That is a threat from outside. What being awake for three days
actually does is from the inside, and it is the best documented thing on this whole list:
**a tired pair of hands is a worse pair of hands, and the person they belong to is the last
to know it.**

- **−0.2 on anything you treat.** The fifth thing that decides an operation, after the room,
  the instrument, the second pair of hands and what this pair has just been doing. And the
  only one of the five that cannot be fixed in the next thirty seconds.
- **Twice as easy to catch what is going around.** The one thing everybody already knows
  about not sleeping.
- **Your own wounds take half again as long to knit.** Mending is mostly something a body
  does while it is asleep.

The first day is free: one late night is not a medical fact. After that it counts towards
the third, and on the second and the third it is **said** - a rule nobody is ever told about
is a rule nobody learns.

**Nothing is stored for any of it.** The counter is vanilla's own, the one the phantoms read:
it already survives a relog and is already reset by the one thing that should reset it. A
second clock beside it would only be a way for the two to disagree.

## You can see the instrument now

Dirt on the saw and the forceps **was visible nowhere at all** until this version. A hidden
number deciding more than half the chance of leaving something in the next wound is not
difficulty, it is the mod keeping a secret. It is on the item tooltip now: "Clean", how many
patients since it was cleaned, and "Filthy" in red once there is nowhere worse to go.

## After the operation

An operated part is now **knitting**, and nothing hurries it. There is no item for this and
there is not going to be one.

**It is not a debuff.** Nothing is taken away while it lasts - a mending arm swings exactly
as well as it did an hour ago. What it is, is a wound that has not finished being one.

- **Sprint on a mending leg, swing or mine with a mending arm**, and it **comes open
  again**: heavy bleeding, the clock back to zero, and somebody has to close it a second
  time on a person who has already paid for it once.
- The trunk is worked by both - there is no way to sprint or swing that a belly full of
  stitches is not part of.
- The head is never worked.
- **Lying low is not a faster clock.** It is the absence of the thing that resets it, and
  that is exactly why this is not a debuff.

The length is decided the moment it starts: **45 seconds** for a job done well in a proper
theatre, up to **135** for a bad one on a floor. That is the second thing a good theatre
buys, and the one people notice.

The part gets its own mark on the chart, and a line in `/triage info` with what is left.

## The mod now runs on 1.20.1 Forge as well

The same mod, not a cut-down one: seven parts, organs, prosthetics, every minigame, every
effect, both languages, the guide. What differs is only what has a different name on 1.20.1
- where the body is stored, how packets travel, how an item remembers what is in it.

One thing there had to be built by hand: block-breaking speed with a broken arm. On 1.21 an
attribute does that and 1.20.1 has no such attribute, so the port works it out on the
`BreakSpeed` event - **60% slower with two unusable arms**, and an arm no longer pays for it
twice.

Both versions ship under one number with the same contents.

## Fixed

- **The jointed leg could not be crafted at all.** Its recipe had four rows and a grid has
  three, so it failed to parse and silently never existed. Same ingredients - leather, two
  iron, redstone - in a shape that fits: the socket on top, the joint in the middle, the
  shin below it.
- **The 1.20.1 port shipped no recipes, advancements or loot tables.** Its data pack
  folders were still in 1.21 form (`recipe` rather than `recipes`, and so on), and nothing
  warns about that - the game simply finds nothing there. The port script carries these
  across now along with everything else.
- **The 1.20.1 port would not start on a dedicated server at all.** Its packet handlers
  were written as method references to client classes, and a method reference resolves its
  target immediately - so the server tried to load a screen class it is forbidden to load,
  and the mod failed to construct. Single player never showed it, because there that class
  is present.
- **The two builds disagreed about what a broken arm was worth.** The port averaged the
  two arms - one dead arm cost half the block-breaking penalty - where 1.21 takes the worse
  of them and charged all of it. The port now reads the arms with the same code 1.21 does.
- **`/triage reset` did not clear what is not in the codec.** The body came back whole
  while its owner went on crashing off an injury they no longer had. Found by the live test.
- **Influenza could not be caught first.** It passed off a sneeze from somebody who
  already had it and arrived from nowhere else - so in a world with one player in it the
  virus did not exist. A disease that cannot start is a disease that is not in the game.
- **Influenza always ended in pneumonia.** It ran its five minutes and turned into it, for
  everybody, every time. That is worse than it looks: it makes the course mandatory rather
  than a decision, it makes the virus a pneumonia timer rather than an illness, and it takes
  away the only interesting sentence in the whole feature - that most flu is just flu. It is
  a roll now, and what it is rolled against is the body.
- **The caught-from message read its arguments in a different order in each language.** The
  English one had them backwards, which produced "you caught Dev off Influenza". Both are on
  indexed placeholders now.
- **The syringes were missing from the creative tab.** Three items existed, crafted and
  worked, and there was no way to find them in creative.
- **The syringe minigame dropped the game to thirteen frames.** Its preview was drawn a
  pixel at a time inside the render loop - around eighteen thousand quads a frame. The
  drawing is scanline bars and polygons now: around a thousand.
- Four dead translation strings left over from earlier renames.

## Config

| | |
|:---|:---|
| `effects.theatre` | off - a treatment is the same anywhere, with anything |
| `effects.placeQuality` | 0.25 - what the place is worth (x0.75 ... x1.25) |
| `effects.grimeInfection` | 0.55 - the chance off the filthiest possible instrument |
| `effects.roomInfection` | 0.18 - and what the worst possible place adds on its own |
| `effects.mending` | off - a treatment ends when it ends |
| `effects.mendingSeconds` | 90 - the base; a job done well is half of it |
| `effects.mendingTearChance` | 0.10 per second, and only while the part is being worked |
| `effects.assist` | off - the second person in the room is scenery again |
| `effects.assistQuality` | 0.15 - what a pair of hands is worth |
| `effects.assistAwakeCap` | 0.7 - the ceiling on operating awake, while held |
| `effects.fatigue` | off - a body weighs nothing after the first step |
| `effects.fatigueSeconds` | 90 - seconds of carrying, with sound arms, before they go |
| `effects.fatigueRecoverSeconds` | 45, halved standing still |
| `effects.fatigueQuality` | 0.25 - what gone arms take off a treatment |
| `effects.fatigueCarrySlow` | 0.5 - how much heavier a body feels by the end |
| `effects.fatigueHold` | 0.15 - what bracing a patient through one treatment costs |
| `effects.adrenaline` | off - a wound hurts from the tick it happens |
| `effects.adrenalineSeconds` | 25 - how long you do not feel it |
| `effects.adrenalineRelief` | 0.8 - what share goes quiet; structure never does |
| `effects.adrenalineCrashSeconds` | 20 - and how long the bill takes to pay |
| `effects.adrenalineCrashBite` | 0.3 - how much worse than normal while that lasts |
| `effects.adrenalineShake` | 0.15 - what the shaking takes off anything you treat |
| `effects.adrenalinePulse` | 30 - what it adds to the pulse |
| `effects.epinephrineRounds` | 2 - rounds of compressions the shot hands back |
| `effects.epinephrineBleed` | 0.06 of volume per part already bleeding |
| `effects.epinephrineArrestChance` | 0.35 - chance a second shot stops the heart |
| `effects.morphineSeconds` | 75 |
| `effects.morphineDepresses` | off - it is a stronger painkiller and nothing else |
| `effects.morphineStopsBreathing` | 0.4 - on a chest that cannot afford it |
| `effects.naloxoneShockPain` / `naloxoneShockChance` | 3 and 0.5 |
| `effects.referredPain` | off - they name the part the injury is actually in |
| `effects.chill` | off - a body opened in a blizzard is a body opened indoors |
| `effects.chillPerOperation` | 0.3 - what one cut costs in the worst place there is |
| `effects.chillDegrees` | 2.5 - what a body that cold reads on the forehead |
| `effects.chillHypothermia` | 1.0 - past which it stops being a reading |
| `effects.chillWarmSeconds` | 90, halved by firelight |
| `effects.sneezeChance` | 0.04 per second, while ill |
| `effects.sneezeDamage` | 0.8 - what one costs a chest with a broken rib |
| `effects.coldCatching` | 0.25 - chance somebody nearby catches it; 0 turns spreading off |
| `effects.pneumoniaAfterSeconds` | 180 - after which a cold can go down into the chest |
| `effects.pneumoniaChance` | 0.01 per second, and only while still cold or wet |
| `effects.pneumoniaDamage` / `pneumoniaSepsisSeconds` | 0.06 a second and 300 to sepsis |
| `effects.peritonitisDamage` / `peritonitisSepsisSeconds` | 0.12 a second and 240 to close the hole |
| `effects.fluSeconds` | 300 - how long influenza runs |
| `effects.fluRiskPerSecond` | 0.0008 a second in a crowd; 0 and it only travels off a sneeze |
| `effects.fluCrowd` | 3 - villagers within eight blocks that count as a crowd |
| `effects.fluPneumoniaPerSecond` | 0.005 by the end of it, and only on a body cold, wet or short of sleep |
| `effects.stomachSeconds` | 240 |
| `effects.stomachChance` | 0.35 - the share of what is eaten badly that was alive rather than poison |
| `effects.stomachDrain` / `stomachDamage` | 0.004 volume and 0.04 to the belly per second |
| `effects.sleepless` | off - a week awake is a week like any other |
| `effects.sleeplessDays` | 3 - days awake at which it is as bad as it gets; the first is free |
| `effects.sleeplessHands` | 0.2 - what a pair of hands that far gone takes off a treatment |
| `effects.sleeplessIllness` | 1.0 - how much likelier they are to catch what is going around |
| `effects.sleeplessMending` | 0.5 - how much longer their own wounds take to knit |
| `effects.thermometer` | off - the number is free again, as it used to be |
| `effects.rinse` | off - water is water and a saw is a saw |
| `effects.rinseSeconds` | 6 - seconds in water per patient rinsed off |
| `effects.rinseFloor` | 1 - how far water reaches; 0 and a stream does the whole job |

Infection from the theatre obeys the general `infection.enabled`: turn that off and this is
off too, however much dirt is on the saw.

# 1.5.0

Two things in this mod have always been done to a patient who is awake and can feel every
part of it: a surgical kit opening them up, and a saw taking their leg off. The mod did not
mention it. The patient lay there and watched.

It mentions it now, and the mention has a price.

Anaesthetic heals nothing. It buys the difference between an operation and a fight - and it
is the first item here whose whole value is what it lets somebody **else** do properly:
nobody can put themselves under and then operate, because being under is being unable to do
anything. A surgeon alone with a kit can still cut into themselves. The mod will simply tell
them what it cost.

And it is the most dangerous thing in the box. The dose that stops somebody feeling an
operation is a short way from the dose that stops them breathing. Nothing else in here can
kill a patient by working too well.

Drops onto 1.4.0. Old worlds and old bodies load as they are.

## New

**Operating on somebody awake.** The body is moving, and no amount of skill fixes that.

- **The quality is capped.** However well the game went, an operation without anaesthetic
  cannot go better than half.
- **The part pays for it** - three on top of the operation itself.
- **And the pain alone may be enough**: an even chance of shock.

Both ends are told. The patient learns they were opened up awake; whoever was holding the
knife learns that they felt all of it.

**Being under** is not the downed state: there is no clock, there is nobody to revive, the
person is simply not in the body.

- **No moving and no swinging** - the same as being on the floor, for the same reason.
- **The lights are off.** Not a penalty; it is the thing that was paid for.
- **They can be carried.** Carrying only ever cared about not being able to walk.
- **It wears off on a clock** and they get up by themselves, unless something else is
  holding them down.

**The mask** is the only game in the mod with **no lit band anywhere**. An open-drop mask is
given the way one was actually given: a drop at a time onto gauze over the face, with your
eyes on the patient.

- **Eyelids moving, breathing quick** - not enough.
- **Breathing slow and even, lying still** - that is it, hold them there.
- **Breathing slower, and a gap where a breath should have been** - too far.

A drop goes on per click, one at a time: a held button would make it a slider with the
numbers hidden, and the whole point is that each drop is a decision taken while looking at a
face. The depth falls the entire time - ether leaves through the same lungs it went in
through - so anaesthesia cannot be set and left.

Nothing fails on the screen. Giving too much is not a fail state, it is a **result**: it
goes out as a bad number and the patient stops breathing, which is a state this mod already
has an answer to - hands on the chest.

**The item.** Anaesthetic: a glass bottle, a fermented spider eye, sugar and a bandage.
Four to a stack. The village medic sells it at master, next to the saw.

## Taking things off again

You could put anything on. You could take almost nothing off: a dressing stayed until it
was replaced, and a strap came off only by spending a bandage on it. Changing your mind
cost a consumable.

**A dressing, by hand.** Free, available to anybody, and the second thing in the mod after
pulling an arrow that can leave somebody **worse** than it found them.

A fresh dressing lifts off. One that has been on a while is stuck to the wound by the very
thing that was trying to close it, and fingers take that with them - so an old dressing is
not only dirty, it is harder to get off. Peel the edge back and the cloth lifts behind your
hand; the stuck patches are drawn, because they are visible in life too. Slowly and it comes
clean. Hurry and it takes the wound with it, and both of you are told.

After two minutes (`dressingStuckSeconds`) it counts as dried on, and the odds of tearing it
open by hand go from about a third to nearly certain.

**Or with scissors.** Bandage scissors: the whole of the item is the blunt lower blade,
which lies flat on the skin and slides along under the cloth, so what is cut is above it and
what matters is below. They do not care how long it has been on or what it has dried onto.

- **They never tear anything.** That is the entire answer to why you would carry a pair.
- One way to do it badly: **lift the blade off the skin**, because a blunt tip stops being
  blunt the moment it stops lying flat. That is a nick and nothing else.
- **Not spent.** The second tool in the box after the saw, and the same bargain.
- Two iron ingots. The village medic sells them from apprentice.

**A strap, by hand.** Nothing goes wrong and nothing needs to: what happens is what the
strap was stopping. The wound under it was never closed - that is the whole of what a
tourniquet is. No screen opens for it, because untying a knot is not worth playing.

## Fixed

**A tourniquet costs the limb it is on, and only that limb.** This was my mistake in 1.4.0.
The 0.3 a second was the whole cost of a strap and it was right while it was the only one.
Then necrosis gave the strap a three minute clock and left the damage running underneath it -
and not only on the limb: whatever the limb could not absorb ran into the chest, the way it
does for a blow. A strap round a forearm was hitting a ribcage. **Nobody ever reached the
clock**, because there was nothing left to reach it with.

It now goes **straight onto the strapped part and nowhere else**. A tourniquet does nothing
whatever to a chest, and billing one for the other was the bug.

The rate is 0.05 a second, which the clock outlives: nine over three minutes, so a healthy
limb is badly used by the time it dies and never quite empties on its own. What empties it is
the deadline, not the drip.

**And letting a long strap go costs something.** The real complication of a tourniquet, and
the one nobody expects: a limb with no blood moving through it does not simply wait. Things
build up in it, and the moment the strap comes off they go everywhere.

- **Nothing at all for the first third of the clock.** A strap put on and taken off in time
  is a strap that did its job, and it costs what it always cost.
- **Past that the whole body pays**, and worse the longer you left it, with a chance of shock.
- **A bandage over the wound releases the strap too**, and is charged the same - otherwise
  the commonest item in the game would be a way round it.

Which is the argument a tourniquet actually makes: at some point the question stops being
when to release it and becomes **whether to release it or cut below it**. That is the road
this mod already has a saw at the end of.

## And you can see it

The third and last thing a body says about itself out loud, after the triage mark and the
missing limb.

A tourniquet is a decision with a clock on it, and the clock could only be read by walking
over and opening somebody's chart - which is the wrong place for it, because the person
wearing one may be in no state to be asked. The strap is now **on the body**, higher up the
limb than a dressing because that is where a strap goes, and it **darkens** as its time runs
out. You can see from the doorway that somebody's is due.

## Inside

This mod has always been about a body being seven places rather than one number, and for
three of those seven the interesting thing was never the meat. A chest is not a thickness
of muscle with a rib in it; it is a heart and two lungs in a box. A belly is not padding.

What that buys is the injury the mod did not have and trauma medicine is mostly about:
**the one that does not bleed anywhere you can see.**

A hard enough blow to a torso tears something inside it, and from that moment the blood is
going somewhere nobody can see - into the belly, into the chest, anywhere but out.

- **Nothing appears on the figure.** No drip on the floor, no stain.
- **No dressing touches it**, and no strap either: there is nothing on the outside to close.
- **It never clots.** The volume falls and does not come back, because the leak has not gone
  anywhere.
- **It is on the chart.** The chart has never lied and will not start. But it can only be
  found by **reading** - it cannot be found by looking, and that is the whole of it. It sits
  beside the crushed lung, which works the same way for the same reason.
- **One thing ends it, and it is a surgeon.** The only injury in the mod with no other
  answer at all.

**A bruise inside** settles by itself in two and a half minutes. **A tear** does not. That
is the difference between a scare and a deadline.

The threshold is about the same blow that breaks a bone, which is not a coincidence: a rib
going in and a spleen going are the same kick from different angles.

### Eight organs, not "something inside"

It started as one place per body part, and the chart could say exactly "something in there
is torn". That is about as useful as a car that says "engine".

There are eight now, and each one is a different emergency:

| | | |
|:---|:---|:---|
| **Heart** | chest | x2.0 - faster than anything else, and it does not give you three minutes |
| **Lung** (left, right) | chest | x0.8, and it crushes itself as well - the needle between the ribs |
| **Spleen** | belly | x1.6 - a bag of blood a kick can burst |
| **Liver** | belly | x1.4 - big, soft, sitting right under the ribs |
| **Kidney** | belly | x0.9 |
| **Gut** | belly | x0.6 - bleeds least, frightens most |
| **Brain** | head | x0.3 - the one thing here a surgeon cannot put back |

The multiplier is against the same per-second loss. **A spleen and a gut are both "bleeding
inside", and one of them is twice the hurry.** That line is the reason for all of this.

The gut has its own price: it does not bleed, it **spills**, and some minutes later that is
sepsis. A torn lung sets up a pneumothorax, which the mod already has an answer for.

### The figure turns over

Not a separate page: **a button in the top right corner of the figure itself**, and it
shows you the same person from the inside. Same place, same size, same coordinates -
switching is turning a body over rather than going somewhere and coming back.

The button's square is red if anything is torn and amber if something is only bruised, so
you never have to look on spec.

And it is **anatomy, not swatches**: every organ has its own shape in its own place. The
heart sits between the lungs and left of the middle, with the two bumps on top and the
point underneath. The liver is a wedge under the ribs on the right, thick at the flank and
tapering away across the midline. The spleen is a small oval on the other side. The kidneys
flank, the gut fills the bottom. Behind all of it, faint ribs and the line they stop at.

Sides follow the figure: the left arm is drawn on the left, so the left lung is too - and
the liver, which lives on the body's right, comes out on the right. Getting that backwards
would teach the wrong lesson perfectly.

- **A tear beats.** The only thing on the page that moves, because it is the only thing
  happening right now.
- **A bruise is amber and still.**
- **Hovering** gives the organ, the part it is in, its bleed multiplier, and what that
  particular organ is frightening for: a gut for what comes out of it, a lung for crushing
  itself.

## Better prosthetics

A wooden leg and a hook were the whole of what was at the end of the worst road in the mod.
There is more of it now.

**A jointed leg** and **a mechanical hand**. The entire difference is one part: **a joint**.
A peg is a stick that meets the ground and everything above it has to swing round it; a
jointed one bends where a leg bends. That gives back **0.75** instead of 0.45 - most of the
difference between walking and getting about. A hook against a hand that opens is the same
argument.

- **They go on a bare stump or over a plain one** - upgrading is what anybody would do.
- **You can see which one somebody is wearing:** the jointed leg is a thigh and a shin with
  a brass knee between them, the hand has three fingers and a thumb. Different silhouettes,
  not different paint.
- Leather, iron and redstone. The medic sells them at master.

That number will never reach one. The mod has exactly one permanent thing in it, and
softening it to nothing would be taking it back.

## A pulse

Everything else in this mod is read off a chart. A pulse is not. It is the first thing
anybody does to anybody, it needs no item and no light and no time, and it works on
somebody still standing up.

And it is **the only outward sign of the injury that has none**. A torn spleen puts
nothing on the skin - no drip, no stain, nothing a dressing could go on. A body losing
blood answers by driving the heart faster. So:

> **a fast thin pulse on somebody with nothing open on them** is the tell, and it is the
> same tell it is in life.

The mod says where the blood went. The pulse says that it went.

### What moves it

The big term by far is blood, and upwards. Everything else nudges:

| | |
|:---|:---|
| **Blood lost** | +85 at the full - the compensation the whole check exists for |
| **Shock** | +25, and thready on the spot |
| **Downed** | +15 |
| **Pain** - fractures, burns, dead limbs | +5 each, capped at +20 |
| **Hypothermia** | **-25** |
| **Anaesthesia** | -15 |
| **Painkillers** | -8 |
| **Cardiac arrest** | nothing under your fingers |

Hypothermia is the cruellest and the most honest of those: a body going cold slows down.
**A slow pulse on somebody who ought to be racing is worse news, not better.**

### The game

The only one in the mod whose reward is information rather than a repair, so it is built
the other way round from all of them. Nothing improves because it went well. What goes
well is that you come away knowing the number; what goes badly is that you come away
knowing only "fast".

There is no target and no bar. There is a place on the wrist and beats arriving **at the
rate the patient's heart is actually running at**, and the whole job is to hold still on
the spot while enough of them go past. Slide off and there is nothing to count.

Which means a player who does this properly **hears the tachycardia before they are told
about it**: the sound arrives at the rate, and a racing heart sounds like one.

- **Held steady** - you get the number: "128, thready".
- **Hand kept sliding** - you get the word: "fast, weak", which is most of what the
  number was for.
- **Nothing you do can make anybody worse.** Inventing a way for it to, so the game would
  feel like the others, would have been a lie about what taking a pulse is.

### Where it lives

- **A free action** at a wrist - either arm that is still there. **With both gone, the
  neck**, which is where it is taken in earnest anyway.
- **Its own line on the chart**, under the volume, because the two are one reading: half
  the volume and a steady seventy is a body that has stopped compensating, and that is
  worse news than the fast one beside it.
- A page in the guide.

## Being near an explosion

Until now the mod treated a blast as a big fire: damage smeared over the body, nothing
broken, nothing bleeding, nothing to treat. That is **the one injury pattern trauma
medicine has a whole vocabulary for**, and the mod had none of it.

A blast does **four things at once**, and they are not the same injury.

### The wave

First, and the one that matters. Pressure does **nothing to muscle** - what it destroys is
anything with gas in it, lungs first and bowel after.

And it leaves **no mark at all**. No wound, no burn, nothing on the floor. Somebody walks
away from a blast talking and drowns in their own chest later.

- **Tears lungs** - each separately, because one torn lung and two are different
  emergencies and the difference should be luck rather than a rule. A torn one sets up a
  pneumothorax on the spot.
- **Tears bowel** - the one that kills slowly, through sepsis.
- **Concussion** - a head rattled without being touched.
- The only sign between the blast and the end is **a breathing rate that will not settle**.
  That case is why breathing was added.

### The fragments

Several small wounds at once, in whatever was facing it. **Same problem an arrow is, same
answer** - pulled out by hand. Only the word differs: "three arrows in a leg" after a
creeper went off is a lie about what happened, so the chart, the pip on the figure and
`/triage info` all call them fragments. The last one out takes the word with it.

### Its own game - the wound in section

An arrow is one thing, you can see it, and the whole question is which way it comes. There
are several fragments, they are small, and each is at its own depth. That is a different
job, so it is a different game.

The wound is drawn **from the side, in section**: skin along the top, tissue below it, and
every fragment sitting at the depth it is actually at. Depth stops being a number to
discover and becomes **a place on the screen** - the one way of showing it nobody has to be
taught.

All the difficulty is in the grip:

- **Grip builds while you hold**, and how much it needs depends on how deep the fragment
  is. The bar marks the **window** where it has come loose: early and wide for one lying
  under the skin, late and narrow for one that has gone in.
- **The window shows before you commit** - just put the tool over a fragment. Deciding
  whether to go after a deep one should be a decision, not a guess.
- **Let go before the window** and it slips, and tears on the way out.
- **Hold past it** and you **crush it, and now there are two.** The only mistake in the mod
  that makes more work rather than less, which is why it lives in a game about patience and
  nowhere else.

The whole handful comes out in **one go** - opening a wound five times to take five pieces
out of it would be five wounds.

### And the rule the whole thing exists for

> **Armour stops fragments and does nothing for the wave.**

Plate is a very good answer to a piece of metal travelling at somebody and **no** answer at
all to the air around it. Full diamond takes 85% of the fragments and **none** of the wave.

Which produces the hardest casualty in the mod: **somebody in full armour who walked out of
the centre with no wound on them anywhere and two torn lungs.** There is nothing to examine.
The only way to find them is the chest.

### Configuration

| | |
|:---|:---|
| `effects.blast` | off - an explosion goes back to being a big fire |
| `effects.blastWave` | off - a blast is fragments and heat and nothing else |
| `effects.blastFragments` | off |
| `effects.blastArmourStop` | 0.85 - how much of the fragments full armour stops |
| `effects.blastLungChance` | 0.45 per lung, at the centre |
| `effects.blastGutChance` | 0.25 |
| `effects.blastBurnChance` | 0.5 |

## A limb can be taken off you

A stump has always been **somebody's decision**: a strap somebody left on, a saw somebody
picked up. There are two ways now that are nobody's.

**A blast takes a limb off.** Only at the centre and only sometimes even there - a rule
that takes an arm off at the edge of a radius makes explosions unplayable; one that takes
an arm off at the centre makes standing next to a charge a thing people do once. Armour
does not help, for the same reason it does not help against the wave.

**And what it leaves is worse than a saw leaves.** A cut is straight and a surgeon chose
where; this is a tear, there is no strap above it because nobody knew, and it is the only
wound in the mod that arrives already emptying somebody.

**The second way: dead flesh is not attached to anything.** A hard enough blow knocks a
necrotic limb clean off. Which quietly turns the necrosis clock from a deadline into a
warning - past the end of it you are not only carrying something useless, you are carrying
something that can be knocked off you.

| | |
|:---|:---|
| `effects.blastTearOff` | off - a blast can only break what it finds |
| `effects.blastTearChance` | 0.35 dead in the middle |
| `effects.tearOffBlow` / `tearOffChance` | 6.0 and 0.5 - what knocks a dead limb off |

## Rabies

Every other illness here is a race you can join late. A fever gives you minutes, sepsis
gives you a fight, and both can be answered **after** they have announced themselves.

This one cannot, and that is why it is here. It is the only condition in medicine where
the treatment works perfectly **right up until the first symptom** and then does not work
at all, ever, for anybody.

- **It comes from a bite** - a wolf, a fox, a cat, an ocelot, a bat, a polar bear. You are
  told once that the animal was not well and never reminded again.
- **Five silent minutes.** No sign, no test. A course of antibiotics in that time takes it
  away completely.
- **Then it starts**, and from that moment a course means nothing. Water turns your
  stomach, the light is unbearable, and ninety seconds later it is over.

Which makes it the one reason in the mod to treat somebody who is **not ill**, and the
answer to the question every player asks once: what happens if I wait and see.

## Forceps

The third tool in the bag that is carried rather than spent. Not a treatment - it never
appears on the medicine screen and does nothing on its own. It is a pair of jaws, and what
it changes is a job you can already do badly with your fingers.

- **Fingers reach what is lying just under the skin.** Everything they have to dig for,
  they tear.
- **Jaws hold what fingers cannot**, and hold it as long as it takes: twice the window.

Iron and three nuggets. The medic sells them.

## The fragment game is a wound now

It used to be a grey rectangle with dots in it. The difficulty was never the problem - the
problem was that **nothing on screen said what you were looking at**, so pulling something
out of it meant nothing.

Now it is a section: skin as a thin lid, dark red muscle under it **going darker with
depth** - and that is not decoration, it is the scale. A fragment three shades down is
three shades deep. The wound is **torn rather than cut**: the edges are ragged and turned
back. The fragments are metal, the one thing in the picture that is not part of anybody,
so they are the one thing with a hard edge and a highlight.

## The voices can be heard

Schizophrenia was entirely text, and a symptom you only read is a symptom you do not have.
Now you can hear it.

**Twenty recordings of a real whisper.** They are synthesised speech with **the voice taken
out**: the mouth keeps the same shapes so the spectrum keeps its peaks, but nothing is
periodic any more - every phase has been thrown away and replaced. That is physically what
a whisper is, and it is why the words are nearly but not quite intelligible. They are not
meant to be. The words are on the screen; this is the thing underneath them.

Both voices installed on the machine were used, so the whispers differ.

**Three times the lines:** 12 to 40 voices from nowhere, and 8 to 24 that arrive with
somebody else's name on them. Twelve lines is a loop, and a loop stops being a voice.

## Phantoms

Three more things a figure can be doing, and the best two are new in kind.

- **Standing with its back to you, not moving.** Held three times longer than anything
  else. A figure walking about is somebody; a figure standing perfectly still with its
  back to you for half a minute is the thing people describe, and it costs nothing to
  draw.
- **Backing away.** It keeps its distance backwards: walk at it and it withdraws. Worse
  than being followed - a thing that follows can be got away from, and a thing that keeps
  its distance cannot be got to.
- **Copying you.** You walk, it walks; you stop, it stops. Nothing about it is aimed at
  you, and that is why it works: it is not chasing, it is copying, and a copy is harder to
  explain away.

## Blood you can see on yourself

Everything in this mod worth seeing from across a field was made visible one at a time -
the mark on a chest, the arm that is not there, the bandage, the strap. Bleeding was the
one left, and it needed the least arguing for. **Blood on somebody is not information you
walk over and read off a screen.** It is the thing you see first and from furthest away,
and it is why anybody walks over at all.

Now it is on the model, on the part it is coming out of. Press F5 and you can see
**where**.

**Painted, not built.** The first attempt stuck red slabs on the limbs and they looked
like exactly what they were: red boxes. Blood is not a shape, it is a stain - ragged at
the edge, running downhill, thinning as it goes and hanging in a drop at the end - and
none of that survives being made out of cubes.

So it is a texture in the skin's own layout, hung on the pieces the model already wears
over the skin: the hat, the jacket, the sleeves and the trousers. Vanilla inflates those a
quarter of a pixel, so the stain sits **on** somebody instead of fighting them for the
same surface, and no new geometry exists anywhere.

**Light and heavy are told apart by how much of it there is, not by shade.** Two reds side
by side are a thing nobody can rank. A seep is the same stain with most of it taken away.

**Nothing is drawn under a dressing** - and a clean dressing no longer drips on the floor
either. A bandaged man used to leave a trail with nothing showing on him.

## Its own particles, instead of redstone

Blood was redstone dust: small glowing cubes. They read as magic because magic is what
they are made of, and a mod about bodies cannot borrow the sparkle off a redstone torch to
say somebody is dying.

- **The drop** - three drawings, and they are the three stages of one fall: it stretches
  as it picks up speed and the drawing changes with the length. It falls and accelerates
  and it does not drift.
- **The stain** - three different splashes with thrown-off droplets round the edge, so a
  trail is not one mark stamped forty times.
- **A drop that reaches the floor stops being a drop** and leaves a mark.

## Blood on the ground

The trail on the floor was already there. What changed is what it looks like.

- **A stain lies flat.** It used to be an ordinary particle, and an ordinary particle
  turns to face the camera - so a mark on the ground **stood up and watched you** as you
  walked round it. A red disc at ankle height is nothing like something spilt. Now the
  quad is turned onto its back and spun to a random angle, because forty marks all lined
  up the same way is a stencil rather than a trail.
- **Standing still leaves a pool.** A mark landing on one already there does not become a
  second mark, it makes the first bigger. Which gives you a line of drops for walking and
  one growing pool for stopping, without either being written as a rule - and where the
  pool is, is where somebody stopped being able to walk.
- **It dries.** Fresh is red and wet, old goes brown and thins out - the only thing a
  trail can say about **which way** somebody went. Follow the fresh end.

**And it lasts longer.** The drying time is up from 45 seconds to **90**. Forty-five is
not a trail, it is a hint: by the time somebody has worked out which way the marks lead, the
far end of them has gone and the direction went with it.

The cap on how many marks a world holds went up with it (240 to 500): a mark a second for a
minute and a half is ninety of them from one person, and a cap a single casualty can reach
on their own quietly shortens everybody else's trail.

`bleeding.bloodParticles` and `bleeding.stainSeconds` still own the switch and the timer, as
they always did.

## Deafened

The injury **everybody** near an explosion has and nobody counts. No treatment and none
needed - it wears off in about ninety seconds.

**And you actually go deaf.** Not a line on a chart: the world switches off.

- **At the moment of the blast you hear almost nothing** - four per cent. Not silence, a
  wall: you can tell something happened and not what.
- **Hearing comes back gradually, and late.** The curve is squared, so it stays gone for a
  while and then arrives.
- **The ringing is one continuous tone.** Not a beep every few seconds - that would be a
  smoke alarm, not a blast. Loudest where there is nothing else to hear, and gone with the
  rest of it.
- **The interface stays audible.** A screen you are looking at is not a sound across a
  room, and taking its clicks away would be taking the game rather than the hearing.

While it lasts it takes away **the one sense this mod uses to warn you about yourself**:

- **You cannot hear your own heartbeat.** That was the only warning in the mod that arrives
  whether or not you were looking - and a blast takes it from the person who was closest to
  it. Nothing replaces it: the point of losing a sense is that it is gone.
- **A pulse has to be counted by eye.** The beats in the game stop sounding; the trace
  stays. It can still be done, it is simply harder - which is the correct amount harder.
- **And a deafened patient cannot hear the question.** The only channel that says **where**
  stops working on exactly the casualty who was nearest the blast, and whose worst injury
  leaves nothing to see.

That last one is the point. The wave leaves no mark, so there is nothing to find but the
chest - and you cannot ask, because the same blast took their hearing.

| | |
|:---|:---|
| `diseases.deafnessSeconds` | 90; 0 turns it off |

## Breathing

A pulse has to be felt for. Breathing does not: you watch the chest and you have the
answer from across the room. The cheapest reading in the mod and the first one anybody
actually gets - which is exactly its place in the order everybody is taught: **airway,
breathing, circulation**, because that is the order they kill you in.

And it does for a crushed lung what the pulse did for internal bleeding. A pneumothorax
has **never had an outward sign**. It sat on the chart under a name most players do not
know, and the item that answers it is the least obvious in the mod. From outside it looks
like **fast and shallow** - and now that is visible.

| | |
|:---|:---|
| **Crushed lung** | +10, and shallow on the spot |
| **Blood lost** | +14 at the full |
| **Broken ribs** | +5, and shallow too - a rib is the one bone whose job is a movement |
| **Shock** | +4 |
| **Downed** | +5 |
| **Anaesthesia** | -5 |
| **Painkillers** | -2 |
| **Blocked airway, cardiac arrest** | not breathing |

**Slow outranks shallow.** Nine breaths a minute is a chest about to stop, and that is
worse news than a chest working badly. It is the only vital here spoiled by **the person
treating rather than by the injury**: the distance between a patient who cannot feel an
operation and one who has stopped breathing is a few drops. Printing "easy" over nine
breaths would have hidden the one number the mask exists to be judged by.

## Temperature

The slowest vital and the only one that **sees the future**. A pulse and a chest say what
is happening now. A temperature says what is going to happen: it starts climbing while an
infection is still a colour on the chart and long before it is a fever with a name.

So somebody who feels a forehead every so often catches **the thing that kills quietly**.

| | |
|:---|:---|
| **Infection** | +0.7, severe +1.4, and +0.3 for each extra one |
| **Sepsis** | +2.0 |
| **Fever** | +1.3 |
| **A dead limb** | +0.8 - the hottest thing in here, and not by accident |
| **Blood lost** | **-2.2** at the full |
| **Hypothermia** | -4.0 |

The second half is the one people get wrong, and it is the important one. **Lost blood and
cold blood both pull it down, and cold blood does not clot.** A casualty going cold is a
casualty whose bleeding is about to stop being something a bandage can answer. Both ends
of that loop were already in the mod. This is the number that shows it closing.

Degrees rather than a bar: thirty-seven is fine and thirty-nine is not, and nobody needs
that explained.

## Asking where it hurts

Every other reading here is taken **off** a body. This one comes **out** of one, and it is
the only one that does.

Which makes it the only thing that says **where**. A pulse says the blood went somewhere.
A conscious person with a torn spleen says **which side of their belly** it went into.
That is how that injury is found in the world: by asking, and believing the answer.

- **Only works on somebody conscious.** Downed, under, arrested - they say nothing. That
  is not a refusal, it is the answer: a patient who cannot say where it hurts is a patient
  you now have to find it on.
- **A rattled head lies.** With concussion, delirium, schizophrenia or shock they name
  something that hurts rather than the worst thing. You are told the answer came from one,
  which is as much help as anybody gets in life.
- **Inside outranks outside.** A torn organ hurts more than the cut over it - otherwise
  asking would only ever confirm what the chart already showed, and the whole channel
  would be decoration.

And the best part: **painkillers work**. A dosed patient reports less than is true: the
small complaints go entirely and the big ones come down a grade, so the belly that was
unbearable is now an ache.

You keep **the location and lose the urgency** - and urgency is the interesting half to
lose, because urgency is what decides who gets treated first. So you choose between
somebody in pain who tells you everything and a comfortable one who tells you half.

Nothing else in the mod makes a treatment cost information.

## Where they live

All three sit on **one row** of the chart, under the volume, because they are read
together. The row says only what is worth saying: a chest doing its job and a normal
temperature print their number and nothing else, so the words on that line are always the
news.

Plus three guide pages and lines in `/triage info`.

### A ribcage is a shield, and a broken one is not

The mod has said from the start that a rib going in and a spleen going are the same kick
from different angles. Now that has the consequence it always should have had.

**A blow to a torso where the bone is already broken tears something inside twice as
often.** A crack counts for half. An intact ribcage stops nearly all of it.

That quietly turns a broken rib from a thing that hurts into a thing that has stopped
protecting you, and gives the splint a second reason to exist that nobody has to be told
about: **the second blow is the dangerous one.**

### Prosthetics wear out

Nothing made of wood and leather and worn all day stays the shape it was cut to. A socket
loosens, a strap stretches, a joint develops play.

A worn prosthesis is **not a broken one, it is a worse one**. It never falls off and it
never demands anything urgent. It simply gives back less of what it gave back on the day it
was fitted: forty minutes of wear takes the ratio down to **0.55 of its own** - a peg from
0.45 to 0.25, a jointed leg from 0.75 to 0.41.

- **One message, at 70%** - the point where it has stopped being the thing that was fitted
  and started being a thing that needs seeing to.
- **The fix is what fitted it:** another prosthesis on the same stump. Fresh is fresh.
- **A fresh one over a fresh one is refused** - the screen says there is nothing to replace
  rather than quietly spending the item. Replacing opens up at half worn.
- The stump gets its own mark on the chart.

That puts a small standing job on the one permanent thing in the mod, which is the right
place for it. Losing a leg is not an event somebody gets past. It is a thing they then live
with, and living with one is mostly maintenance.

## Configuration

| | |
|:---|:---|
| `effects.anaesthesia` | off - an operation goes back to saying nothing about the patient |
| `effects.anaesthesiaSeconds` | 90 for a good dose, a fraction of it for a poor one |
| `effects.anaesthesiaArrestChance` | 0.35, scaled by how poor |
| `effects.awakeQualityCap` | 0.5 |
| `effects.awakeDamage` | 3.0 |
| `effects.dressingStuckSeconds` | 120 - past this a dressing has dried on and fingers are dangerous |
| `bleeding.tourniquetDamagePerSecond` | 0.05, onto the strapped limb only - no overflow to the chest |
| `effects.reperfusion` | off - a strap comes off free however long it was on |
| `effects.reperfusionDamage` | 7.0 at the full clock, spread over the body |
| `effects.organs` | off - there is nothing inside a torso to tear |
| `effects.organBlow` / `organChance` | 11.0 and 0.30 - how hard a blow, and how likely |
| `effects.organBleedPerSecond` | 0.007 - between a light bleed and a heavy one, and it never ends |
| `effects.organDamagePerSecond` | 0.12 into the part itself |
| `effects.organHealSeconds` | 150 - the bruised kind only; a tear waits for a surgeon |
| `effects.prostheticRatioFine` | 0.75 |
| `effects.pulse` | off - a heart is something you read off a chart again |
| `effects.breathing` | off - a chest stops being something you can read |
| `effects.temperature` | off - a forehead tells you nothing |
| `effects.ask` | off - nobody says anything |
| `effects.organBrokenBonus` | 2.0 - how much likelier a tear is under broken bone; 1.0 turns it off |
| `effects.prostheticWearSeconds` | 2400 - forty minutes of wear to completely worn; 0 turns it off |
| `effects.prostheticWornRatio` | 0.55 - what a loose one is worth against a fresh one |

# 1.4.0

Everything the mod did to a body could be walked back. Enough bandages and enough
patience undid any of it, which quietly made every decision in it smaller than it looked:
a tourniquet was a free way to stop bleeding, a festering arm was a chore to get round to.

Both of them have a clock now. Neither is a surprise: the strap says on the chart that it
is on, the rot says on the chart that it is spreading, the warning arrives out loud with a
third of the time left, and the count runs back down the moment somebody deals with the
cause. There is exactly one way to reach the end of it, and that is to keep not dealing
with it.

What is at the end is not death. It is a limb that has died on a living person - nothing
treats it, nothing mends it, and it poisons what it is attached to. There is one answer to
that, and it is the first treatment in this mod that leaves the patient permanently
smaller than they were.

And it is **visible**. Everything else the mod knows about a body lives on a chart,
because bleeding is a thing you read about somebody. An arm that is not there is not: it
is gone off the body itself, on everybody watching, in the first person and under whatever
armour they are wearing. And what got fitted in its place is there to see.

The other half of the version is the hands it is all done with. Seventeen treatments shared
eight games between them, and almost every one of those tested a wrist: hit it, hold it, be
quick. Almost none of these treatments is lost on any of that. Every one has its own now,
none of them is shared, and each is about what that treatment actually is.

Drops onto 1.3.0. Old worlds and old bodies load as they are.

## New

**The tourniquet has a clock.** It stopped bleeding and cost the limb 0.3 a second, which
made it very nearly free. In life it is the hardest deadline in field medicine, and the
only reason anybody puts one on is that they have agreed to lose what is under it.

- **Three minutes** (`tourniquetDeadSeconds`). That is how long an arm or a leg lives
  under a strap.
- **A minute before the end you are told out loud**, with a heartbeat behind it. Not a
  line in the corner.
- **The count runs back down at the same rate.** Take it off in time and the limb has lost
  nothing at all. It is a deadline, not a punishment.
- The chart shows the clock: `/triage info` reads "dying: Ns", and so does the limb's row.

**So does rot.** A severe infection in a limb that nobody cleaned gets to the same place
in **four minutes** (`rotDeadSeconds`). If both are running, the shorter one is the one
that counts - anything else would mean guessing which of two invisible counters you are
racing.

**A dead limb.** The first state in the mod that nothing repairs. Not a dressing, not a
medkit, not a surgeon.

- **It has no health and never will.** Everything that was wrong with it - the break, the
  burn, the arrow, the rot - comes off with the diagnosis, because none of that is a wound
  any more. One word is left on the chart.
- **It poisons the body.** 0.2 a second into the chest, half again what a blacked-out limb
  costs, and a standing chance of the blood going bad. That is the argument for the saw,
  and it is the argument the real operation is made of: the limb is not the patient.
- Arms and legs only. A stomach is not on the list: a body without one is not a body
  anybody keeps playing, and the mod has no answer for it that is not a slower death.

**The saw.** The only treatment here that removes something instead of repairing it.

- **Only on a limb that has already died.** It is never the clever shortcut - by the time
  it is an option, everything else has stopped being one.
- **The game is strokes.** Full ones, end to end, and level: a saw worked in the middle of
  its length binds. Eleven of them. It is not hard, it is long, and that is the only honest
  way a game can say what this is.
- **It cannot be failed.** There is no state of the world in which putting it down halfway
  is better than finishing, so the game does not let you put it down. Crooked strokes are
  charged to the patient - up to twice the volume - but the operation gets finished.
- **The saw is not spent.** Everything else in the box is a dressing or a dose. A saw is a
  tool, and the question this operation asks is whether to do it at all rather than whether
  there is one left in the bag.

**A strap above the cut is the whole rule.**

- **With one** there is a stump with an ordinary bleed that clots like any other.
- **Without one** there is an artery open on it that never will, and **a fifth of the
  volume** gone at once. It is the fastest way to kill somebody in this mod, and it is
  meant to be.

It costs volume either way and it puts the patient into shock either way. What it buys is
that everything the dead limb was taking out of the chest stops the moment it comes off.
That is the entire trade.

A tourniquet now goes onto a dead limb as well as a bleeding one. Without that the rule
was impossible to follow: there is nothing running through a dead limb to stop, and the
strap that killed it comes off the moment it dies. This is the one that goes on **before**
the saw, and putting one on is exactly what it is for.

**With no legs you do not stand.** Sawing both of your own off and walking away was
possible, and it should not have been.

There is nothing to stand on now - and it is **not** the downed state: nobody is dying, no
clock is running and there is nobody to revive.

- **With Sable: Ragdolls you drop into a ragdoll.** The mod already puts downed players on
  the floor that way, and this is the same path rather than a second one like it.
- **Without it, the same pose a downed player gets.** You crawl: speed at the slowness
  ceiling, jump at zero. Not jumping badly - not jumping.
- **Your hands still work.** A legless surgeon is still a surgeon: treat yourself, treat
  other people, dig, swing. You cannot walk.
- **You can be carried.** Carrying used to need somebody downed; now it needs somebody who
  cannot walk, which was always the only thing it was about.
- **One wooden leg gets you up.** That is the whole argument for having built them.

Legs blacked out but still attached hold you up as before. That is deliberate: a surgeon
brings a blacked-out limb back in ten seconds, and nothing brings back one that is gone.
One is a bad afternoon, the other is the rest of the game.

**Prosthetics.** A **wooden leg** and a **hook**. Neither is the limb and neither pretends
to be.

- They give back **about half** of a working one (`prostheticRatio`). That half is the
  difference between walking and dragging.
- Fitted to a stump that has **stopped bleeding**, once.
- Made out of what anybody has: planks and leather, iron and leather. The point of them is
  that they exist - without them the worst road in the mod ends in a wall.
- The village medic sells both at expert and the saw at master. Finding one for sale is a
  great deal kinder than finding out what they are for.

**Phantom limb pain.** The limb is gone and it still hurts. Nearly everybody who loses one
has this.

It does nothing: no damage, no penalty, no clock. It is here for the same reason it is in
every account of the operation - the chart says the arm is not there and the person keeps
reporting the arm. Painkillers work on it, which is also true, and is the only thing
anybody has ever been able to offer for it.

**You can see it on the body.** The most permanent event in the mod was the least visible
one. Not any more.

- **The part is simply not drawn** - on the player, on everybody around them, and in the
  first person.
- **Under armour too.** Armour is a second humanoid model with its own arms, and it decides
  for itself which of them to show; left alone it puts an iron sleeve exactly where the arm
  is not, which reads as a bug rather than as a wound. The vanilla armour layer is replaced
  with the same thing plus one line.
- **Nothing is held in a hand that is not there.** A pickaxe floating at a shoulder is the
  sort of thing that makes everything round it look unfinished. A hook keeps its item: the
  mod does not take anything out of anybody's inventory for this, and drawing an empty hand
  for somebody holding a sword would be the render lying about the game.
- **The prosthetic is drawn.** A leg out of planks, a hook out of iron, both hanging off
  the same joint the limb hung off - so both swing with the walk, because they are hanging
  off the same joint.
- It travels on the same broadcast as a triage mark, for the same reason: an arm that is
  not there is not something anybody has to walk over and read. It is the shape of a
  person, and it is visible from across a field.

**Dying puts it back**, the way dying puts everything else back. The body is not marked
`copyOnDeath`, so a respawned one is new - arms, legs and all. That is not a mercy, it is
the same rule that already applied to fractures and sepsis, and it has just got more
expensive.

## Every treatment is its own pair of hands

Surgery cleaned its field with the antiseptic's game at higher difficulty. The painkiller,
the antibiotics, the course and the needle between the ribs all shared one mark to hit. A
prosthesis was fitted with the splint's bone game followed by the bandage's winding game. It
read as procedures and it was a playlist.

The point is not that there are eighteen games now. The point is that each is about what
that treatment actually is, and almost every time that turned out to be **not** what the
game had been testing.

The bandage tested a wrist and knew nothing about the dressing, the overlap or the tension.
The splint taught players to straighten a broken limb, which is the one thing every manual
tells you not to do in the field. Compressions were a rhythm, and compressions are lost on
depth and recoil. Pressure was a force gauge, and pressure is decided by the point and by
not lifting your hand to look. The antiseptic asked people to scrub a wound, which is not
done to wounds at all.

**Bandage.** The dressing goes on the wound before the roll comes out. Then the roll is
carried out past one side and swept **across** the limb - that is one turn, laid at exactly
the height the roll is at - and a lit band on the arm shows where the next one has to land:
**each turn over half the last**. Further apart and it is open between them. Same place and
the roll has gone nowhere. Snatched and the gauze tears, and that turn goes on crooked and
stays crooked.

**The tourniquet comes through the same class and inverts nearly all of it:** above the
wound, between it and the heart; it must not travel, because a strap that spirals is a
bandage and occludes nothing; and it cannot be pulled too hard, because too tight is the
whole idea. Below it the limb goes blue.

**Splint.** The old game had you pulling a broken bone back into line. A field splint goes
on **as you find it** - hauling on a broken bone is how a closed fracture becomes an open
one. Two things decide it and both are duller: the board **has to be long enough**, because
a support that stops near the break holds nothing, and the ties go **above and below, never
across**. Both are places on a limb, so both are lit on the limb - a band for the middle of
the board, ticks for how far the ends must reach, then two bands for the ties with a
conspicuous gap between them. The gap is the break. **A crack and a fracture are now drawn
differently:** a break is jagged, across, with the halves out of line; a crack is a thin
line along the bone with no displacement.

**Medkit.** A wound is a **line**, not a set of points. The edges are drawn together from
one end to the other: slowly down it, each stretch holding after the hand has been on it a
moment. Dragging quickly closes nothing. Pull away and the last stretch comes apart.

**Surgery.** An operation, once you are inside, is a fight for the view. The field fills so
you cannot see; you swab it clear and find what is filling it; you clamp that and it fills
more slowly. Two hands, two jobs, in a loop, and the patient pays in volume for every
second. **A click on a vessel is a clamp and a click on anything else is a swab** - there is
no mode. Then it closes, and each stitch is the needle **in one side and out the other**,
with both ends checked.

**Antiseptic.** You do not scrub a wound. You hold the bottle over it and **flush**, and the
dirt comes out of the channel onto the skin - which is what irrigation does. Then you wipe,
and **always away from it**. Every speck moves with your hand: outwards and it goes,
inwards and it comes back into the wound with you.

**Salve.** A burn is the injury where the surface is the injury. Do not rub it, and do not
break the blisters - the roof of a blister is the best dressing that wound will get. The
salve goes on where the hand passes, slowly; past a speed it is rubbing and lays nothing.
The blisters are lit; touch one and it bursts, and the raw patch round it has to be covered
too.

**Painkiller and antibiotics.** Tablets are a **number**. Tip the bottle and they come;
tip it further and they come faster. It finishes when there are exactly that many **and the
bottle is upright again** - without which the last tablet would end it the instant it landed
and pouring too many, the only mistake tablets have, would be impossible. Extra ones go back
in one at a time. Antibiotics use the same bottle with the **dose set by severity**: a
course for a cold and a course for sepsis are not the same thing, and it takes three marks
under the palm instead of two to say so.

**The course.** The mod's own comment said "a card of tablets rather than a bottle" and then
drew the needle game. A blister pack counts nothing out - you **press it through the foil**.
The shortest game in the mod, and it should be: this is a treatment somebody is *on*, and
its weight is in the six hundred seconds afterwards.

**Drip and blood.** Everything else is done with the hands and finished. A line is set up
and then **watched**. Drag the roller clamp; the rate **does not stay where you put it** -
a bag empties, a line settles, a vein does what veins do - and a marker shows where the
drops actually are rather than where the clamp is. It is read off the **chamber**, not a
number. Progress only moves while the rate is in the band. **Drawing blood is the same line
run backwards.**

**The needle between the ribs.** The mark used to hang in mid-air; the whole difficulty is
that the place is defined by **two ribs**. **Over the top of the lower one, never under the
upper** - the vessels and the nerve run in a groove on the underside of each rib. The space
and the groove are drawn, and going in is an advance rather than a click: the needle goes
until something changes. What changes is the hiss.

**The arrow.** The old game knew one answer: draw it back. **A broadhead has barbs**, and
dragging one back tears everything on the way. There are two right answers and the arrow
tells you which: **if the point is tenting the far skin**, push it on through and take the
head off out there; **if it is not**, cut across the entry first so the barbs have somewhere
to come through, and only then draw. The one game here where the first thing to do is look.

**The saw.** You do not cut through at one level. You cut a **flap** below the line the bone
will be divided at, draw the soft tissue back, and saw the bone **shorter than the flesh** -
then there is something to close the stump with. Everything at one level is a guillotine and
leaves nothing to cover the bone. Trace the flap, the tissue retracts, and the bone is left
standing proud of it.

**Prosthetics.** Two things decide whether one is worth wearing, and the first is not
generally known: **a socket must not bear on the end of the bone** - the load goes on the
sides of the stump. Pushed on to the tip it is unwearable; barely on it rocks and rubs.
The second: **a leg set crooked is a limp for as long as it is worn**, so it is lined up
against **the leg that is left**, drawn beside it. The hook has the same question turned
round: which way it faces.

**Compressions.** Everybody knows the rhythm. Compressions are lost on the other two.
**Depth** - shallow ones are not worse, they are nothing. **Recoil** - the hands must come
all the way off, because the heart only fills while the chest is back out, and leaning
between compressions is the commonest fault there is and invisible to the person doing it.
A compression is now a press **and** a release, both measured, on a beat. Ribs go at the
bottom of one that went too far.

**Pressure.** It has to be **on the point**, not on the pool: blood runs where gravity takes
it and the middle of the mess is almost never the middle of the hole. The source is not
marked - what gives it away is the same thing that gives it away in life. **And do not lift
your hand to look**: whatever was forming comes away with your fingers.

**The airway.** The tilt and the sag were right and are still here. Two halves were missing.
**Look first** - if there is something in the mouth it is hooked out **sideways**, not
chased in with a finger. **And check at the end**: the chest only moves once air is getting
in.

## And a dressing is a thing on somebody

**A dressing stays on, and goes bad.** A bandage used to be an event. Now it is a thing on
somebody. While it is fresh it is the best item in the box - **nothing under a clean dressing
can go bad at all**, the only preventive thing in the mod. Left on for eight minutes it
becomes what is keeping the dirt in, and from that moment the wound can turn with no waiting
at all. The answer is another bandage; the dirty one comes off with it. Antiseptic takes it
off too, because cleaning a wound means opening it. It comes off by itself once there is
nothing underneath worth dressing.

**And it is on the body.** Seven places, one band each: the head, two round the trunk, the
forearms and the shins. Cream while it is fresh, the colour of a dressing nobody has changed
once it is not. It travels on the same broadcast as a triage mark and a missing limb.

**The recovery position.** The only treatment in the mod that keeps working after you have
gone. On their back the tongue goes the way gravity takes it; on their side it does not, and
their airway closes at **half the rate** - not for twenty seconds, but for as long as nobody
moves them. It is done in a particular order: the **far** arm and the **far** knee, with the
knee as the handle, so the weight comes onto the shoulder and not the face. Then the head
back and the top hand under the cheek, which is what holds the head tilted once nobody is
holding it. A blow rolls them back.

## Fixed

- **`triage.disease.delirium`** was never in the language files, so delirium showed as a
  raw translation key in chat, on the HUD and on the chart from 1.3.0 onwards. Found in
  live testing.
- **A prosthetic could get infected.** Infection looked for a part at zero health, and a
  limb that is not there is at zero health by definition - so a stump, and the wooden leg
  screwed to it, went into the same list as a blacked-out arm. Neither rots: there is
  nothing left in one and the other is a plank. The same applied to a dead limb, which
  `die()` clears on purpose and which was quietly festering again a minute later. Bodies
  already carrying one are put right on the next tick.
- **The saw, the wooden leg and the hook had no tooltips** - a raw translation key showed
  instead of the text.
- **A stump kept a dressing forever**: "health below maximum" is always true of a limb that
  is not there. A stump is dressed while it is open and no longer.

## Configuration

| | |
|:---|:---|
| `effects.necrosis` | off - a strap goes back to being a slow cost with no deadline |
| `effects.tourniquetDeadSeconds` | 180 |
| `effects.rotDeadSeconds` | 240 |
| `effects.deadLimbDrain` | 0.2 a second into the chest |
| `effects.deadLimbSepsisChance` | 0.01 a second |
| `effects.amputation` | off - a dead limb stays on the body for good |
| `effects.amputationBloodLoss` | 0.12 |
| `effects.amputationOpenBloodLoss` | 0.2 on top, with no strap above the cut |
| `effects.prosthetics` | off - nothing to fit |
| `effects.prostheticRatio` | 0.45 |
| `effects.phantomPain` | off - the body forgets |
| `effects.dressings` | off - a bandage goes back to being an event and nothing is tracked |
| `effects.dressingCleanSeconds` | 480 |

# 1.3.0

Everything the mod could offer a dying player was an item. Turn up without the right one
and there was nothing to do but stand there and watch the clock, which quietly made this
a mod about inventory rather than about people: the question was never who is here, it
was who packed a surgical kit this morning.

Real first aid is the other way round. The things that save the most lives cost nothing
and need nothing but a person: open the airway, push on the chest, hold the wound shut,
get the arrow out. All four are here now. None of them heals anything. All of them have to
be done again.

And the clock has a second half. Reaching zero used to be the death; now it is the heart
stopping, which is a different event - the body is still there, and the whole question is
whether anybody is standing over it.

Then the version went somewhere the mod had never been. The chart grew a guide that does
not remember any answers and asks the mod for them instead. The screens stopped being a
dark panel with a border and became the thing the mod is named after - a printed casualty
card, on either of two papers. And the list of things that can be wrong with somebody
gained, for the first time, one that did not happen to them: an illness they arrived with,
and everything it brings.

Drops onto 1.2.0. Old worlds and old bodies load as they are.

## New

**A player on the floor stops breathing.** Nobody holds their own airway open. Twenty
seconds after they go down with nobody minding it, air stops getting in - and the clock
runs twice as fast. It is the largest single thing that can be done for somebody dying on
the ground, and it costs nothing, which is exactly how it is in life.

**Opening it needs a person, not an item.** Empty hands, the button at the bottom of
their chart, done. The count goes back to zero and starts again, so this is not a thing
you fix once - it is a thing you keep coming back to. It is the only treatment in the mod
that cannot be carried in a bag.

**You can hear it.** Everything else the mod knows about a body belongs to that body and
has to be gone and read on purpose. Somebody who cannot breathe is not a line on a chart,
it is a noise from the floor, and anybody near enough to help is near enough to hear it.

**The clock running out stops the heart instead of ending the player.** There are
twenty-five seconds after it in which somebody can still get them back. What gets them
back is still the surgical kit - the window does not save anybody, it buys the time for
somebody to arrive.

**Your heartbeat goes silent.** The beat added in 1.2.0 was the one thing the mod said
without words; now it is the one thing it says with silence. Everybody nearby hears one
last beat, and that is the only time anybody else can hear your heart at all.

**Compressions are the other free thing your hands can do.** They do not restart the
heart, and that is the design rather than a shortcut: compressions that occasionally
stood somebody up would make the surgical kit optional and the arrest a slot machine.
Compressions that only buy time make it a job somebody has to keep doing while somebody
else runs for the kit. That is two people, which is what the mod is for.

**Every round buys less than the last** - 25 seconds, then 15, 9, 5, 3, 1 - so one pair
of hands is worth about a minute and a half in total and never more. It is the same
counter the dressings already carry, for the same reason.

**And every round is another go at their ribs.** Thirty per cent a time. A chest pushed
hard enough to move blood is a chest that cracks, and they wake up with the evidence.
That is the price of the answer, not a punishment for giving it.

**Pressing again while the last round is still holding is refused, not charged for.** The
first version counted every press, so a player who did not know the rhythm simply held
the button down, got nothing twelve times over a window that was already longer, and
ended up with a patient nobody could hold any more.

**A blocked airway drains the arrest window at the same doubled rate.** Pushing blood
round a body no air is reaching is pushing the same blood round. It also gives a second
pair of hands somewhere to be: one on the chest, one on the head.

**A dressing does nothing in arrest and says so.** What holds a clock cannot start a
heart. The thing that body needs is a pair of hands, and those are free.

**Everything else about being down still applies**, because arrest is still being down:
mobs still lose interest, the ragdoll still holds, a blow still costs time rather than
health, and they can still be carried.

**A bare hand on a wound stops it bleeding while it is there.** The first thing anybody
is taught, and the last thing this mod had: before it, a bleeding teammate and no bandage
was a bleeding teammate and nothing at all. It closes nothing - let go and it runs exactly
as it ran - but a light bleed goes on clotting underneath, so one held long enough does
close without a dressing. The hand goes where the blood is; you do not pick a limb off a
list while somebody empties out in front of you. It is also the only one of the three that
does not wait for them to fall over.

**None of the three is a button.** Compressions have exactly one number attached to them
in life and it is a rate, pressure has a force, and an airway has an angle you find and
then hold. A button knows none of that, so each of them has its own game now - the
shortest games in the mod, because all three are done over somebody who is dying.

**The rhythm, the force, and the angle.** Compressions sweep a marker and the beat is the
middle of it, a hundred and ten a minute, six pushes. Pressure climbs while the button is
down and falls while it is up, and the work is sitting in the band. The head follows the
cursor and sags back out of the angle the moment nobody is holding it, and too far back is
as shut as not far enough.

**None of them can be failed.** Failing would mean it was a mistake to walk over, and it
never is. Doing it badly is worth less instead: a sloppy press holds for five seconds
rather than twelve, a lost rhythm buys less of the arrest window, and a half-found airway
has to be found again sooner. It never drops below a third - somebody who turned up gets
something.

**Painkillers have a game now too.** They were the last item in the box without one -
swallow, done. Shaking a dose out of a bottle is a thing your hands do. They still cannot
be interrupted, which was always the half of that argument that was actually about being
ill rather than about being busy.

**A guide, on the chart, under a question mark.** The chart said what was wrong and never
what to do about it. Nineteen pages: what the thing is, what it does to you, and what
answers it.

**The guide does not know the answers - it asks for them.** Every page about a limb builds
the state it describes and puts it to the same method the treatment screen uses to grey out
parts not worth treating. A guide written in prose would be wrong within two versions, and
would still be recommending a splint for a skull. This one cannot say that.

**An arrow can stay in.** Not extra damage - the damage happened on the way in - but a
wound nobody can deal with while it is there: it will not clot, it will not mend, and a
bandage over the top of it is a bandage over an arrow, which the mod says out loud rather
than quietly spending the dressing. About a third of the arrows that hit you stay in.

**Pulling it out is free, and it is the only thing in the mod that can leave somebody
worse off than before you helped.** Done carefully it leaves an ordinary cut; jerked out
it tears, which is a heavy bleed and damage on top. Waiting for steadier hands is a real
plan, and nowhere else in this mod was doing nothing an option.

**And it is the one you can do to yourself.** The other three are done to somebody who
cannot help themselves. An arrow in your own arm is not that.

**None of the four is a button any more.** Compressions have exactly one number attached
to them in life and it is a rate; pressure has a force; an airway has an angle you find
and then hold; an arrow has the line it came in on. A button knows none of that, so each
of them has its own game now - the shortest games in the mod, because all four are done
over somebody who is dying.

**None of them can be failed except the arrow.** Failing would mean it was a mistake to
walk over, and it never is: doing it badly is worth less instead, and never less than a
third. The arrow is the exception for the same reason it is interesting at all.

**Painkillers have a game now too.** They were the last item in the box without one.
Shaking a dose out of a bottle is a thing your hands do. They still cannot be interrupted,
which was always the half of that argument that was about being ill rather than busy.

**Your hands go on a limb, not on a button.** The strip of buttons along the bottom of the
chart is gone: it read as a toolbar, and it meant the player picked the action and the mod
picked the limb - backwards, for a screen whose whole subject is which part of somebody is
wrong. Click the part, on the figure or in the list. One thing to do happens; several open
a short list under the cursor - pull the arrow, or hold the wound. The airway is the head
of somebody on the floor and compressions are the chest of somebody whose heart has
stopped, so every one of them is "put your hands here" again.

**A guide, on the chart, under a question mark.** Twenty pages: what a thing is, what it
does to you, and what answers it. The answers are not written in there - every page about
a limb builds the state it describes and asks the same method the treatment screen uses to
grey out parts not worth treating. A guide written in prose would still be recommending a
splint for a skull two versions from now.

**Every screen is paper now.** They were dark panels with bevelled edges and gradient
bars, which is what every mod menu looks like and said nothing about this one. This mod is
named after writing on wounded people and pinning the result to their chest: the paperwork
is the subject. So the screens are the paperwork - an aged sheet with a hard shadow,
printed rules under every row, gauges filled in rather than glowing, two inks and one
accent. Red means "this one" everywhere now instead of competing with six other saturated
colours. Illnesses used to print in their chat colours, and an aqua "cold" on a cream sheet
was a smudge; they are ink with a severity pip now. The four tag colours are the one thing
left that is not ink, because they are not a readout - they are the tear-off tabs.

**Delirium.** Laid out against ABCDE, the last quarter was covered by concussion alone -
a knock to the head. A head fails for other reasons: it fails when the body stops
supplying it. Delirium arrives from a third of the volume gone, an airway that has shut,
sepsis, or hypothermia, and it has no treatment of its own because it is not an illness,
it is a sign. Fix the cause and it lifts. While it holds you hear things that are not
there.

**Some bodies arrive with schizophrenia.** The first condition in the mod that nobody
caught and nothing caused: it is rolled with the blood type, at the same moment and for
the same reason - a fact about a person rather than an event in their day. One body in
ten by default, and configurable.

**It is not done the way games do it, and that is deliberate.** Real voices comment,
criticise and repeat themselves. They do not hand out orders; the version that does is
the most repeated untruth about this illness, and a mod that leans on being accurate does
not get to borrow it. There is no "kill them" anywhere in here.

**Voices.** One of four things every twenty-five seconds: a noise from where nothing is,
a line of chat with nobody attached to it, a word across the middle of the screen that
fades and never appears twice in the same place, and a message in chat with another
player's name on it that they did not write. The last is built out of the same component
the server would have sent and lands in the same chat line, because being unable to tell
your own thoughts from somebody else's speech is the thing being described, and no game
seems to do it.

**Figures.** Every few minutes somebody is standing about nearby who is not there - seven
times in ten a person wearing the face and name of a player really on the server. It
walks, works, crouches, looks around, sometimes follows at a distance it never closes,
and sometimes turns and watches you, and may say something in chat while it does. All of
it exists on one client: nothing is sent, no entity is in the world, and nobody else sees
or hears any of it. It gives itself away only by not surviving contact - swing and your
hand goes through nothing, walk up and there is nobody there.

**And the chart starts mislabelling.** Two limbs swap names on a slow clock. The bars,
the pips and the numbers stay where they belong, so the information is all correct and
attached to the wrong word - which is a different problem from concussion, where the
answer goes coarse but never wrong.

**The half with no drama in it.** Wanting to do nothing, and a body looked after worse for
it: blood comes back half as fast. Nobody models this because there is no scene in it,
and it is the half that does the most damage in life.

**A course that holds it down, and what that costs.** The antipsychotic cures nothing and
does not pretend to: while you are on it the voices, the figures and the mislabelling stop
almost entirely. While you are on it you are also slowed and permanently hungry, and when
it runs out everything is twice as loud for a minute. The question stops being whether to
treat it and becomes which of the two you would rather put up with, which is the truest
thing this mod can say about it.

**Catatonia.** The illness at its worst: movement is taken away outright and the world
goes dark, while the person is still there taking it in. Nothing in the box touches it and
it passes on its own. It is the second thing in this mod whose only answer is somebody
else being present - and a course prevents it, which is the strongest argument for staying
on one.

**And the course itself can kill you.** Three per cent a dose: a temperature, muscles
locking, damage across the whole body. There is no antidote. Stop the course and get cold
- water, rain, snow - which is the exact inverse of what hypothermia wants, so having both
at once is the worst hand the mod can deal. It is the sharpest form of this mod's oldest
argument: every treatment is a trade, and here is one that can be the thing that ends you.

**A dark sheet.** The paper comes in two now - the same form, the same rules, the same one
accent, printed pale on dark card instead of dark on cream. Every colour is mixed twice
rather than filtered, because ink that reads on cream turns to mud on charcoal. The switch
is beside the question mark on the chart, and it is the only setting in the mod that
belongs to the person looking rather than to the world.

## Fixed

**`/triage info` prints the downed clock, the arrest window and the airway count.** None
of the three is drawn on the figure, so without those lines there was no way to check
them at all. `/triage airway` and `/triage arrest` set them.

**Minecraft 1.21.1 - NeoForge - MIT - English and Russian**

---

# 1.2.0

The mod is named after sorting the wounded, and there was no way to sort anybody: the
only way to find out how badly a teammate was hurt was to start treating them and read
the screen it opened. Now you can just look — and write down what you found where the
rest of the group can read it.

Then it turned out there was not much to sort by. Laid out against ABCDE — the order a
casualty is worked through — the mod covered A, half of C and D. Breathing and exposure
did not exist at all, and neither did shock, which is the other half of C. All three do
now.

And a fracture stopped being one boolean for all seven parts: a rib, a skull and a femur
are three different injuries with three different answers.

Drops straight onto 1.1.0. Old worlds and old bodies load as they are.

## New

**Look at somebody without touching them.** Right-click a player with an empty hand and
you get their chart — every part, every condition, blood left, blood type, whatever
illness is running. Nothing is spent, nothing is started. Shift is still "pick them
up"; without it, it is a look.

It is a snapshot, not a feed: you walked up, you saw what you saw. If you want to know
whether they have got worse, look again. And a concussion applies to the reader, not
the read — a rattled medic makes a poor job of somebody else's chart too.

**Triage tags.** Four colours, the real ones: immediate, delayed, walking, expectant.
You write one from somebody's chart, and from then on everybody can read it off their
nameplate from as far away as a nameplate goes. A downed player's own name turns dark
red, so the floor is legible at a distance too.

The mod never works out which colour is correct and never will — deciding that is the
job. And a mark is a snapshot exactly like the look that produced it: it never updates
itself, and after two minutes it keeps its colour and picks up a question mark. A
yellow tag on somebody who has been on the floor for a minute is not a bug. It is the
whole point.

Tags are paper and string, four to a craft, and the village medic sells them. Only the
person wearing one can tear it off.

**How long somebody has left is on their chart.** The downed clock used to exist only
on the patient's own screen, which meant the one person who could not answer "how bad
is it" was the one standing over them. It shows whether a dressing is holding it, too.

**Three things that were missing.** Laid out against ABCDE - the order a casualty is
worked through - the mod covered A, half of C and D. B and E did not exist at all.

**Shock.** Blood loss used to be a slow penalty you could walk off. What actually kills
people who have been bleeding is circulation giving up, and it kills them after the
bleeding has been stopped. It arrives at a third of your volume, or at two parts gone
at once with nothing leaking at all - that one is the crush injury. It slows and weakens
you on top of your wounds, halves the rate you make blood back, and runs the downed
clock half again as fast, which is the whole reason two people with the same wound have
different amounts of time.

Nothing in the box treats it. It lifts when the cause is gone and only while the patient
stays still, so getting up is what stops them recovering. That is the argument it exists
to have.

**Hypothermia.** The mod had a cold; it had no body temperature. This does almost
nothing by itself. What it does is stop blood setting, so an ordinary cut that would
have closed on its own keeps running instead - which is why the first thing to do with
somebody on a cold floor is get them off the cold floor. Losing blood makes it four
times easier to catch, because a body short of blood cannot keep itself warm. Warmth and
dry clothes lift it; firelight lifts it twice as fast.

**A collapsed lung.** A broken rib pushed the wrong way, and now there is air in the
chest pressing the lung flat. It is the only injury here with nothing to see - nothing
bleeds, nothing is bent - and the only one that gets *worse* the longer it is left, so
it is the shortest deadline in the mod. Only ever after the ribs have already gone,
which is the answer to why a cracked chest is worth splinting.

The treatment is a **decompression needle**: iron, glass and antiseptic. It heals
nothing. It lets the air out, and the chest goes back to being an ordinary damaged chest.

**Every bone is its own bone now.** A fracture used to be one boolean and one splint, so
a rib and a femur and a skull were the same injury with a different word on it.

Ribs break in numbers - a kicked-in chest is three or four separate breaks, and each one
is its own pass with a splint. A skull and a spine cannot be splinted at all: you do not
strap a stick to somebody's back, so those go to the surgical kit, which is the most
expensive thing in the box. Breaking your head now costs more than breaking your arm,
which is the right way round.

The collapsed lung counts them too: every broken rib is another end that can go the
wrong way, so three of them is close to a certainty and a mere crack is worth half of
one. That is the answer to why a cracked chest is worth splinting.

The chart shows one mark per break, and a part that is broken in three places says so.

**Your own bones.** The bone-setting screen drew the same femur whatever you had
broken. It now draws what is actually under that part: a skull as a dome, ribs as bars,
vertebrae as a stack, a slim forearm, a thick femur with a head on it.

**Painkillers stop being free.** Each course inside the window is worth less than the
one before it, and the tolerance only wears off once the last dose is out. The fourth
in a row is barely worth swallowing. They are still the answer to a bad ten seconds;
they have stopped being the answer to everything. Your current tolerance is on the body
screen, so you find out before the pills fail rather than when they do.

**A heartbeat.** Everything this mod tells you arrives as text on a bar or a colour in
the corner, both easy to be too busy to look at. Low on blood, or on the floor, you
hear it instead — faster as it gets worse. Only ever the player it belongs to. Off with
one config line if you would rather not.

**Set somebody down with a right click**, not only with the key — and they are laid out
in front of you rather than dropped at your own feet.

**Optional: real ragdolls.** Lying on the floor here is a forced swimming pose - the same
shape whatever put you there, clipping nothing on the way down. Install
[Sable: Ragdolls](https://modrinth.com/project/I3mWDgfy) and the rest of this section
happens. Without it the pose is still the pose, and none of the code below is even loaded.

**A downed player goes limp.** The body is dropped by the physics engine at whatever speed
it was carrying when it went down, and then it lies where it landed rather than where it
was standing.

**And they cannot climb off it.** That is the oldest rule this mod has — a downed player
does not save themselves — and it used to be held up by zeroed movement and blocked input.
Now it is the same rule in the physics too, rather than two rules that have to be kept in
agreement.

**The doll runs on Triage's clock, not its own.** How long somebody has left is counted to
the second and drawn on their screen; a ragdoll quietly expiring on a timer of its own
would have stood them up in the middle of that count. It is let go exactly where the mod
already lets a downed player go: surgery, giving up, dying, respawning.

**Ragdolls now land on something.** A ragdolled player used to eat a fall entirely -
dropped a hundred and twenty blocks they got up with a scratch, because a passenger takes
no fall damage and the physics mod deals none. Triage measures the drop itself now, off
the player's own height, and deals it.

And it lands on a part rather than on the legs: a body that has been tumbling arrives on
whatever was underneath, most of the blow goes there and the rest travels through. The
bone that breaks is the bone under whatever hit, landing head first is a concussion, and
landing chest first is broken ribs and possibly a crushed lung.

Which part cannot be measured - the ragdoll mod publishes no way to ask where a limb is -
so it is rolled on the ordinary hit weights. A body rolling down a hill really does land
on an arbitrary part of itself, so that is not a stand-in for the truth.

**Carrying still works, which it very nearly did not.** A player on a ragdoll sits in its
seat, which means they are riding something — and "riding something" is how the carry code
asks whether somebody else already has them. With the physics mod installed, every attempt
to pick anybody up would have answered "somebody else has them", silently, with nothing in
any log. Hands beat physics now: the doll is let go first. Set them down and they flop
again rather than standing into a pose. You cannot use the ragdoll key while carrying
somebody, for the obvious reason.

**The H key gives way.** Sable: Ragdolls binds "go limp" to H, and so did Triage's "look
yourself over". Two mods sharing one default is a conflict you have to go and untangle in
the options before either key does anything, so with the physics mod installed Triage's
default moves to **J** — that screen also has a button next to the inventory, and theirs
is the whole mod. Only the default moves; anybody who has rebound it keeps what they set.

Switch it off with `effects.ragdoll` and the pose comes back.

## Fixed

**Carrying somebody looked different to the two of you.** The carrier saw them lying
where they fell; they saw themselves riding the shoulder. The server sends no position
at all for a passenger — every client works it out from the vehicle, and that sum
subtracts the passenger's own attachment point, which comes from their dimensions,
which come from their pose. The downed pose was being forced in two places that did not
agree, so the two clients measured the same offset from two different bodies. Nobody is
posed while they are being carried now; a passenger already looks like one.

**`/triage reset` let go of a downing without ending it** — the only path in the mod that
did. Harmless before this update; with ragdolls it left somebody face down in a heap they
were not allowed to get out of.

**Minecraft 1.21.1 · NeoForge · MIT · English and Russian**

---

# 1.1.0

Blood stops being only the thing you lose, the head stops being only the part that
kills you fastest, and a wounded friend no longer has to be operated on where they
fell. Medicine gets a second source besides the crafting bench.

Drops straight onto 1.0.0. Old worlds and old bodies load as they are.

## New

**Hold somebody without fixing them — the thing the mod is named after.** A bandage or
a medkit on a downed player stops the clock. It does not heal them and it does not
stand them up; it holds, so that a medic a minute away is a medic rather than a
mourner. It cannot become immortality: every further dressing on the same downing
holds for half as long as the last, any hit tears it open, and you cannot do it to
yourself. A rescue is two moves now — hold them, then go and get the surgical kit.

**A splint sets a bone rather than mending it.** A break comes back as a crack: it
knits if you leave it alone and goes again if you do not. The splint is the first half
of a fix and resting it off is the second.

**Carry a downed player out.** Surgery where they fell is often no surgery at all —
the fire is still burning and the thing that put them there is still standing over
them. Shift + right-click with an empty hand and you pick them up: half speed, no
hand free to fight with, and the clock still running, at half pace. **G** sets them
down.

**Blood types, on the real ABO and Rh rules.** Everybody has one and it is rerolled
on every respawn. Blood only goes into somebody who already carries every antigen it
does, which makes **O−** worth hoarding and **AB+** a lucky thing to be. Draw blood
into an empty bag and the donor pays for it out of their own volume. Give a matching
bag and it puts back far more than saline can. Give the wrong one and the body
attacks it — the one thing in this mod that punishes you for treating somebody rather
than for failing to. The type is on the bag, on your body screen and in the header
while you work, so it is never a gamble; only a thing you did not read.

**A medic in the village.** Villages generate a clinic, built out of whatever that
village is built out of, with a medical table inside. The villager who takes that job
sells bandages cheaply and surgical kits and blood bags last of all, and buys string,
leather and sugar cane. Put a medical table down yourself and any unemployed villager
will take the job.

**Concussion.** A hard knock to the head takes away the one thing this mod otherwise
always gives you: knowing exactly how bad things are. While it lasts every number is
a **?** and every bar collapses to one of three lengths. Nothing lies to you — you
work off an impression instead of a readout. No medicine touches it; it wears off.

**Saline drip.** The first answer blood loss has ever had that is not "sit down". It
is refused while anything is still open, so a wound still has to be closed before it
can be undone.

**Two new items and a block:** the saline drip, the blood bag, and the medical table
that makes a villager into a medic.

## Fixed

- **The antibiotics minigame was unreachable.** It had been written, drawn and wired
  up, and never once opened: the screen was gated on "does this medicine need a body
  part", and pills do not. Gated on "is there anything to do by hand" instead, it
  works.
- Saline is drawn as a clear fluid rather than a red one, now that there is blood in
  the game for it to be confused with.

**Minecraft 1.21.1 · NeoForge · MIT · English and Russian**

---

# 1.0.0 — first release

Seven body parts, each with its own health, instead of one health bar.

## What is in it

**A body that can go wrong in six ways.** Bleeding in two grades, blood loss that no
bandage fixes, cracks that become fractures if you lean on them, burns that stop a
part healing, and infection that turns into fever and then into blood poisoning.
Limbs black out instead of killing you, and then drag your chest down until you deal
with them.

**Nine medical items, and none of them work by clicking.** Each one opens a screen
where you pick the part and do the work: wind a bandage round the limb in circles,
hold a broken bone in line while it slips, scrub a wound edge to edge, spread salve
without wasting any. Surgery is three of those in a row.

**Going down instead of dying.** Emptying your head or chest puts you on the floor
with a clock running. Mobs lose interest, anything that hits you anyway takes time
off the clock, and somebody else with a surgical kit can bring you back. Alone, hold
**G** and go on your own terms.

**Treating other people.** Right-click a player with medicine and it is their body on
your screen — their limbs, their bleeding — with your supplies.

**A body screen** on **H**, or the red cross beside your inventory: every part with
numbers, an icon and a name for everything wrong with it, blood left, and any illness
you are carrying.

**Armour still works.** All of it — armour, resistance, enchantments — exactly as
before. The mod only changes where the damage goes afterwards, and adds one rule: a
hit that lands where you are wearing nothing hurts more.

## Taking only part of it

Everything is server-side config and travels with the world. Whole subsystems switch
off on their own: the minigames, the downed state, aimed hits, infection, diseases,
burns.

Solo players may want `effects.downed` off — on your own it is a dramatic last few
seconds rather than a rescue.

**Minecraft 1.21.1 · NeoForge · MIT · English and Russian**
