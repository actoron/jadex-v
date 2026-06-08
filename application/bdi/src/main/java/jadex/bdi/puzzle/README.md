# Jadex Puzzle Application

Solve a puzzle using goal/plan-decomposition.

Main class: [SokratesAgent.java](SokratesAgent.java)


## Description

This example is adapted from the commercial agent
platform JACK(TM) from Agent Oriented Software.
The Jadex implementation is very similar to
allow performance comparison between both
platforms.


### Rules

This is a puzzle game played by one agent.
It consists of a board with white and red
pieces. Objective is to swap the positions
of both pieces whereby the following rules for
making a move exist.

- white pieces move right or down to an adjacent
free field.
- white pieces jump right or down over a red
piece to a free field.
- red pieces can only move up or left with the
same restrictions as white pieces.
- the color of a piece to move is not specified.


### Solution Strategy

The agent uses meta-level reasoning to solve the
puzzle. It creates a goal to make a move and find
the solution. For this goal a plan for each
possible move is created. To decide which move
plan to test first a meta-level goal is created.
The choose move plan handles the meta goal and
decides according to the specified strategy:
- default: use order of movelist from board
- long: prefer long moves
- long_same: prefer jump moves of the same color
- long_alter: prefer jump moves of alternate color
