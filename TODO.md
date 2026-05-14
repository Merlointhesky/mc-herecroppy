### TODO LIST

- save the box setup too, if we aren't yet
- poisonous potatoes will be hardcoded to be dumped in the keep box (we should add a note in the config menu for the player as there is no point to junk it as it cannot be turned to bonemeal)

- ensure the order of action for each farmable block is: 
- - till (if not tilled) 
- - seed (if not seeded)
- - bonemeal (if not ripe)
- - collect ( if ripe )
- - repeat once more from seed step
- - check inventory status and dump if full
- - check bonemeal status and collect if empty
- - move 1 step and restart from till step.

- instead of stating 1 line for each crop collected, we should try display a "table" of the farming status:
- - list the crops collected and the total of seed (where applicable) or crop collected
- - list the number of times we emptied a full inventory
- - list the number of bonemeal used

example:

#HERECROP ACTIVITY LIST:
# WHEAT: 200 seed and 300 Wheat collected!
# CARROT: 200 Carrot collected!
# SUGAR CANE: 100 Sugar Cane collected!
# Emptied inventory 3 times
# Used 600 bonemeal