/**
* Mohan Chowdhury, Andy David Zaruma, Kaiwen Liu
* Professor Bon Sy
* CSCI212 331
*/
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.util.Scanner;

public class DB_Project {
    //global connection objects
    public static Connection connect = null;
    public static PreparedStatement preparedStatement = null;

    public DB_Project() {
	}

    //test and establish connection to MySQL
    public int testconnection_mysql(int offset) {
        String connection_host = "mysql host location";
        ResultSet rs;
        int flag = 0;
        try {
            connect = DriverManager.getConnection("jdbc:mysql://3.145.135.235:3306/chad", "db_user", "Password0!");
            Class.forName("com.mysql.cj.jdbc.Driver");
            String qry1a = "SELECT CURDATE() + " + offset;
            preparedStatement = connect.prepareStatement(qry1a);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                String nt = rs.getString(1); 
                System.out.println(offset + " hour(s) ahead of the system clock of mysql at " + connection_host + " is: " + nt);
            }
            rs.close();
            preparedStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    //method to process inputted problem into the database
    public static void problemEntry(String description) {
        try {
            preparedStatement = connect.prepareStatement("INSERT INTO problems (description) VALUES (?)");
            preparedStatement.setString(1, description); //insert input into SQL statement
            int rowsInserted = preparedStatement.executeUpdate();
			//if row insertion is successful or failed, inform user
            if (rowsInserted > 0) {
                System.out.println("Problem description added successfully to the database.");
            } else {
                System.out.println("Failed to add problem description.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //method to contribute a SQL statement
    public static void sqlEntry(int problemId, String sqlQuery) {
		try {
			try (PreparedStatement stmt = connect.prepareStatement("INSERT INTO sql_contributions (problem_id, sql_statement) VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				stmt.setInt(1, problemId);
				stmt.setString(2, sqlQuery);
				int rowsInserted = stmt.executeUpdate();
				if (rowsInserted > 0) {
					// Retrieve the generated SC_ID (SQL Contribution ID)
					ResultSet generatedKeys = stmt.getGeneratedKeys();
					int scId = 0;
					if (generatedKeys.next()) {
						scId = generatedKeys.getInt(1);
					}
					// Now insert the parameters into the runnable_queries table
					System.out.println("Enter parameters for the SQL query (one by one). Type 'done' when finished:");
					Scanner scanner = new Scanner(System.in);
					
					// Loop to capture parameters
					while (true) {
						System.out.println("Enter parameter name (or type 'done' to finish):");
						String paramName = scanner.nextLine();
						if (paramName.equalsIgnoreCase("done")) {
							break;  // Exit the loop when done
						}
						System.out.println("Enter parameter type (e.g., INT, VARCHAR, DATE):");
						String paramType = scanner.nextLine();
						// Insert parameter info into runnable_queries
						String insertParamSQL = "INSERT INTO runnable_queries (problem_id, sc_id, parameter, parameter_type) VALUES (?, ?, ?, ?)";
						try (PreparedStatement paramStmt = connect.prepareStatement(insertParamSQL)) {
							paramStmt.setInt(1, problemId);
							paramStmt.setInt(2, scId);
							paramStmt.setString(3, paramName);
							paramStmt.setString(4, paramType);
							paramStmt.executeUpdate();
						}
					}
					// Update the problem entry to indicate it now has an associated SQL
					String updateSQL = "UPDATE problems SET has_SQL = 'YES' WHERE problem_id = ?";
					try (PreparedStatement updateStmt = connect.prepareStatement(updateSQL)) {
						updateStmt.setInt(1, problemId);
						updateStmt.executeUpdate();
					}
					System.out.println("SQL query and parameters successfully added.");
				} else {
					System.out.println("Failed to insert SQL query.");
				}
			}
		} catch (SQLException e) {
			System.out.println("Error executing SQL query: " + e.getMessage());
		}
	}

	private static String getSQLQueryById(int scId) {
		String sqlQuery = null;
		try {
			preparedStatement = connect.prepareStatement(
				"SELECT sql_statement FROM sql_contributions WHERE sc_id = ?"
			);
			preparedStatement.setInt(1, scId);  //use SC_ID to retrieve SQL statement
			ResultSet resultSet = preparedStatement.executeQuery();
			
			if (resultSet.next()) {
				sqlQuery = resultSet.getString("sql_statement");
			}
		} catch (SQLException e) {
			System.out.println("Error fetching SQL query by SC_ID: " + e.getMessage());
		}
		return sqlQuery;
	}
	
	private static void executeSQLQuery(int scId) {
		try {
			//retrieve the SQL query and its associated parameters
			String sqlQuery = getSQLQueryById(scId);
			if (sqlQuery == null) {
				System.out.println("Invalid SC_ID selected.");
				return;
			}
			//prepare the SQL statement
			PreparedStatement stmt = connect.prepareStatement(sqlQuery);
			//get parameters for this query from the runnable_queries table
			PreparedStatement paramStmt = connect.prepareStatement(
				"SELECT parameter, parameter_type FROM runnable_queries WHERE sc_id = ?"
			);
			paramStmt.setInt(1, scId);
			ResultSet paramSet = paramStmt.executeQuery();
			//collect parameter values and bind them
			int parameterIndex = 1; //start binding from parameter index 1
			Scanner scanner = new Scanner(System.in);
			while (paramSet.next()) {
				String param = paramSet.getString("parameter");
				String paramType = paramSet.getString("parameter_type");
				System.out.println("Enter value for parameter (" + param + "): ");
				String inputValue = scanner.nextLine();
				//handle parameter type conversion if necessary
				if (paramType.equalsIgnoreCase("INT")) {
					stmt.setInt(parameterIndex, Integer.parseInt(inputValue)); //for integers
				} else if (paramType.equalsIgnoreCase("DATE")) {
					stmt.setDate(parameterIndex, java.sql.Date.valueOf(inputValue)); //for date fields
				} else {
					stmt.setString(parameterIndex, inputValue); //default to string for VARCHAR or other types
				}
				parameterIndex++; //move to next parameter index
			}
			ResultSet resultSet = stmt.executeQuery();
			//display results
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
	
			// Print column names
			for (int i = 1; i <= columnCount; i++) {
				System.out.print(metaData.getColumnName(i) + "\t");
			}
			System.out.println();
	
			// Print rows
			while (resultSet.next()) {
				for (int i = 1; i <= columnCount; i++) {
					System.out.print(resultSet.getString(i) + "\t");
				}
				System.out.println();
			}
		} catch (SQLException e) {
			System.out.println("Error executing SQL query: " + e.getMessage());
		}
	}	

    //method to return data from the database
    public static void readOut(int mode, int id) {
		ResultSet resultSet = null;
		try {
			if (mode == 1) {
				// Display all problems without SQL
				preparedStatement = connect.prepareStatement("SELECT * FROM problems WHERE has_SQL = 'NO'");
			} else if (mode == 2) {
				// Display problem description for a specific problem ID
				preparedStatement = connect.prepareStatement("SELECT description FROM problems WHERE problem_id = ?");
				preparedStatement.setInt(1, id);
			} else if (mode == 3) {
				// Display problems with SQL contributions (information seeker)
				preparedStatement = connect.prepareStatement(
					"SELECT p.problem_id, p.description, sc.sql_statement, sc.sc_id " +
					"FROM problems p " +
					"JOIN sql_contributions sc ON p.problem_id = sc.problem_id " +
					"WHERE p.has_SQL = 'YES'"
				);
			}
			resultSet = preparedStatement.executeQuery();
			// Display results
			if (resultSet.next()) {
				int columnCount = resultSet.getMetaData().getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					System.out.print(resultSet.getMetaData().getColumnName(i) + "\t");
				}
				System.out.println();
				
				do {
					for (int i = 1; i <= columnCount; i++) {
						System.out.print(resultSet.getString(i) + "\t");
					}
					System.out.println();
				} while (resultSet.next());
			} else {
				System.out.println("No data found for the query.");
			}
		} catch (SQLException e) {
			System.out.println("Error executing query: " + e.getMessage());
		}
	}
	
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: java -jar DBTest_Demo.jar <number_of_offset>");
                System.out.println("Success returns errorlevel 0. Error return greater than zero.");
                System.exit(1);
            }
            DB_Project DBConnect_instance = new DB_Project();
            if (DBConnect_instance.testconnection_mysql(Integer.parseInt(args[0])) == 0) {
                System.out.println("MYSQL Remote Connection Successful Completion");
            } else {
                System.out.println("MySQL DB connection failed.");
                System.exit(1);
            }
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== Main Menu ===");
                System.out.println("1. Specify a Problem");
                System.out.println("2. Contribute SQL");
                System.out.println("3. Display a List of Runnable Queries");
                System.out.println("4. Exit");
                int choice = scanner.nextInt();
                scanner.nextLine();
                switch (choice) {
                    case 1:  // Problem Specifier
                        System.out.println("You have selected Problem Specifier. Enter the problem description in plain English:");
                        String description = scanner.nextLine();
                        problemEntry(description);
                        break;
					case 2:  // SQL Contributor
						System.out.println("You have selected SQL Contributor. Below are problems without SQL contributions:");
						readOut(1, 0);  //displays problems without SQL contributions
						System.out.println("Select the problem ID to contribute SQL to, or 0 to return to main menu:");
						int problemID = scanner.nextInt();
						scanner.nextLine(); //consume newline
						if (problemID == 0) {
							break;  //return to main menu
						}
						readOut(2, problemID); //display specified problem description
						System.out.println("Enter your SQL query (use ? for parameters):");
						String sqlQuery = scanner.nextLine();
						sqlEntry(problemID, sqlQuery); 
						break;
					case 3:  // Information Seeker
						System.out.println("You have selected Information Seeker. Below are runnable queries:");
						readOut(3, 0);  //display problems with SQL contributions
						System.out.println("Enter the SC_ID of the SQL you would like to execute, or 0 to return to the main menu:");
						int scId = scanner.nextInt();
						scanner.nextLine();  //consume newline
						if (scId == 0) {
							System.out.println("Returning to main menu...");
							break; //return to the main menu
						}
						String sqlQueryToExecute = getSQLQueryById(scId);  //fetch SQL query by SC_ID
						if (sqlQueryToExecute != null) {
							//execute and display results
							System.out.println("Executing SQL query: " + sqlQueryToExecute);
							executeSQLQuery(scId);
						} else {
							System.out.println("Invalid SC_ID selected. No SQL query found.");
						}
						break;
                    case 4:  //exit
                        scanner.close();
						try {
							if (preparedStatement != null) preparedStatement.close();
							if (connect != null) connect.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}