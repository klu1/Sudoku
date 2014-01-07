package assign3;

import java.util.*;

/*
 * Encapsulates a Sudoku grid to be solved.
 * CS108 Stanford. Kevin Lu.
 */
public class Sudoku {
	// Grid data for main. 
	// 1 solution this way, 6 solutions if the 7 is changed to 0
	public static final int[][] myGrid = Sudoku.stringsToGrid(
	"3 7 0 0 0 0 0 8 0",
	"0 0 1 0 9 3 0 0 0", 
	"0 4 0 7 8 0 0 0 3",
	"0 9 3 8 0 0 0 1 2",
	"0 0 0 0 4 0 0 0 0",
	"5 2 0 0 0 6 7 9 0",
	"6 0 0 0 2 1 0 4 0",
	"0 0 0 5 3 0 9 0 0",
	"0 3 0 0 0 0 0 5 1");
	
	public static final int SIZE = 9;  // size of the whole 9x9 puzzle
	public static final int PART = 3;  // size of each 3x3 part
	public static final int MAX_SOLUTIONS = 100;
	
	// Static utility methods to
	// convert data formats to int[][] grid.
	
	/**
	 * Returns a 2-d grid parsed from strings, one string per row.
	 * The "..." is a Java 5 feature that essentially
	 * makes "rows" a String[] array.
	 * (provided utility)
	 * @param rows array of row strings
	 * @return grid
	 */
	public static int[][] stringsToGrid(String... rows) {
		int[][] result = new int[rows.length][];
		for (int row = 0; row < rows.length; row++) {
			result[row] = stringToInts(rows[row]);
		}
		return result;
	}
	
	
	/**
	 * Given a single string containing 81 numbers, returns a 9x9 grid.
	 * Skips all the non-numbers in the text.
	 * (provided utility)
	 * @param text string of 81 numbers
	 * @return grid
	 */
	public static int[][] textToGrid(String text) {
		int[] nums = stringToInts(text);
		if (nums.length != SIZE*SIZE) {
			throw new RuntimeException("Needed 81 numbers, but got:" + nums.length);
		}
		
		int[][] result = new int[SIZE][SIZE];
		int count = 0;
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				result[row][col] = nums[count];
				count++;
			}
		}
		return result;
	}
	
	
	/**
	 * Given a string containing digits, like "1 23 4",
	 * returns an int[] of those digits {1 2 3 4}.
	 * (provided utility)
	 * @param string string containing ints
	 * @return array of ints
	 */
	public static int[] stringToInts(String string) {
		int[] a = new int[string.length()];
		int found = 0;
		for (int i=0; i<string.length(); i++) {
			if (Character.isDigit(string.charAt(i))) {
				a[found] = Integer.parseInt(string.substring(i, i+1));
				found++;
			}
		}
		int[] result = new int[found];
		System.arraycopy(a, 0, result, 0, found);
		return result;
	}

	public static void main(String[] args) {
		Sudoku sudoku;
		sudoku = new Sudoku(myGrid);
		System.out.println("asdfasdf");
		System.out.println(sudoku); // print the raw problem
		
		int count = sudoku.solve();
		System.out.println("solutions:" + count);
		System.out.println("elapsed:" + sudoku.getElapsed() + "ms");
		System.out.println(sudoku.getSolutionText());
	}
	
	private class Spot {
		private int row;
		private int col;
		
		private Spot(int x, int y) {
			row = x;
			col = y;
		}
		
		private void Set(int num) {
			grid[row][col] = num;
			rowNums.get(row).add(num);
			colNums.get(col).add(num);
			boxNums.get(getBoxIndex(row, col)).add(num);
			spotsToFill.remove(this);
		}
		
		private void reset() {
			int oldNum = grid[row][col];
			grid[row][col] = 0;
			rowNums.get(row).remove(oldNum);
			colNums.get(col).remove(oldNum);
			boxNums.get(getBoxIndex(row, col)).remove(oldNum);
			spotsToFill.add(0, this);
		}
		
		private Set<Integer> getNumsUsed() {
			Set<Integer> possibleSet = new HashSet<Integer>(SIZE);
			possibleSet.addAll(rowNums.get(row));
			possibleSet.addAll(colNums.get(col));
			possibleSet.addAll(boxNums.get(getBoxIndex(row, col)));
			return possibleSet;
		}
	}
	
	private int[][] grid;
	private List<Spot> spotsToFill;
	private List<Set<Integer>> rowNums;
	private List<Set<Integer>> colNums;
	private List<Set<Integer>> boxNums;
	private String solutionString;
	private long startTime;

	/**
	 * Sets up based on the given ints.
	 */
	public Sudoku(int[][] ints) {
		grid = ints;
		spotsToFill = new ArrayList<Spot>();
		solutionString = new String("");
		startTime = System.currentTimeMillis();
		initArrayLists();
		initGridInfo();
		solutionString = new String("");
	}
	
	private void initArrayLists() {
		rowNums = new ArrayList<Set<Integer>>(SIZE);
		colNums = new ArrayList<Set<Integer>>(SIZE);
		boxNums = new ArrayList<Set<Integer>>(SIZE);
		for (int i = 0; i < SIZE; i++) {
			rowNums.add(new HashSet<Integer>());
			colNums.add(new HashSet<Integer>());
			boxNums.add(new HashSet<Integer>());
		}
	}
	
	private void initGridInfo() {
		for (int row = 0; row < SIZE; row++) {
			for (int col = 0; col < SIZE; col++) {
				if (grid[row][col] == 0) {
					spotsToFill.add(new Spot(row, col));
				} else {
					rowNums.get(row).add(grid[row][col]);
					colNums.get(col).add(grid[row][col]);
					boxNums.get(getBoxIndex(row, col)).add(grid[row][col]);
				}
			}
		}
	}
	
	private int getBoxIndex(int row, int col) {
		return (col / PART) + ((row / PART) * PART);
	}
	
	/**
	 * Solves the puzzle, invoking the underlying recursive search.
	 */
	public int solve() {
		int solutions = 0;
		Collections.sort(spotsToFill, new Comparator<Spot>() {
			public int compare(Spot s1, Spot s2) {
				Set<Integer> set1 = s1.getNumsUsed();
				Set<Integer> set2 = s2.getNumsUsed();
				return set2.size() - set1.size();
			}
		});
		solutions = doSudoku(solutions);
		return solutions;
	}
	
	private int doSudoku(int solutions) {
		if (solutions >= MAX_SOLUTIONS) return solutions;
		if (spotsToFill.isEmpty()) {
			if (solutionString.equals("")) setSolutionString();
			return solutions + 1;
		}
		Spot testSpot = spotsToFill.get(0);
		for (int numToTry = 1; numToTry <= SIZE; numToTry++) {
			if (!testSpot.getNumsUsed().contains(numToTry)) {
				testSpot.Set(numToTry);
				solutions = doSudoku(solutions);
				testSpot.reset();
			}
		}
		return solutions;
	}
	
	private void setSolutionString() {
		for (int i =0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				if (j == SIZE - 1) {
					solutionString += grid[i][j] + "\n";
				} else {
					solutionString += grid[i][j] + " ";
				}
			}
		}
	}
	
	public String getSolutionText() {
		return solutionString;
	}
	
	public long getElapsed() {
		return System.currentTimeMillis() - startTime;
	}
	
	private void printGrid() {
		for (int i =0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				System.out.print(" " + grid[i][j]);
			}
			System.out.println("");
		}
	}
}
