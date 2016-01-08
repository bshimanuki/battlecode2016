import rlcompleter
import readline

RUBBLE_CLEAR_FLAT_AMOUNT = 10
RUBBLE_CLEAR_PERCENTAGE = 0.05
RUBBLE_OBSTRUCTION_THRESH = 100
RUBBLE_SLOW_THRESH = 50

def next_rubble(rubble):
    return max(0, rubble * (1 - RUBBLE_CLEAR_PERCENTAGE) - RUBBLE_CLEAR_FLAT_AMOUNT)

def clear(rubble, turns=1):
    for i in xrange(turns):
        rubble = next_rubble(rubble)
    return rubble

def clear_until(rubble, amount):
    turns = 0
    while rubble >= amount:
        turns += 1
        rubble = next_rubble(rubble)
    return turns

def until_pass(rubble):
    return clear_until(rubble, RUBBLE_OBSTRUCTION_THRESH)

def until_fast(rubble):
    return clear_until(rubble, RUBBLE_SLOW_THRESH)
