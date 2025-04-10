package Payroll;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the Employee # to search for: ");
        String searchEmployeeNumber = scanner.nextLine().trim(); 

        System.out.print("Enter the start date of the week (MM/dd/yyyy) \nStarting from 06/03/2024 - 12/31/2024 : ");
        String startDateString = scanner.nextLine().trim();

        String csv1Path = "src\\CSV1.csv";
        String csv2Path = "src\\CSV2.csv"; 

        Map<String, String[]> employeeInfoMap = new HashMap<>();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        Date startDate = null;
        try {
            startDate = dateFormat.parse(startDateString);
        } catch (ParseException e) {
            System.out.println("Invalid start date format. Please use MM/dd/yyyy.");
            scanner.close();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DATE, 7);
        Date endDate = calendar.getTime();

        try (BufferedReader br1 = new BufferedReader(new FileReader(csv1Path))) {
            String line;
            br1.readLine();

            while ((line = br1.readLine()) != null) {
                String[] values = line.split(",");
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].replaceAll("\"", "").trim();
                }

                if (values.length > 14) {
                    String employeeNumber = values[0];
                    String lastName = values[1];
                    String firstName = values[2];
                    String birthday = values[3];
                    String basicSalary = values[15];

                    employeeInfoMap.put(employeeNumber, new String[]{firstName, lastName, birthday, basicSalary});
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean found = false;
        Map<String, Long> totalWorkedMinutesMap = new HashMap<>();

        try (BufferedReader br2 = new BufferedReader(new FileReader(csv2Path))) {
            String line;
            br2.readLine();

            while ((line = br2.readLine()) != null) {
                String[] values = line.split(",");
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].replaceAll("\"", "").trim();
                }

                if (values.length > 5 && values[0].equals(searchEmployeeNumber)) {
                    String employeeNumber = values[0];
                    String date = values[3];
                    String logIn = values[4];
                    String logOut = values[5];

                    Date recordDate = null;
                    try {
                        recordDate = dateFormat.parse(date);
                    } catch (ParseException e) {
                        System.out.println("Invalid date format in CSV2 for employee " + employeeNumber);
                        continue;
                    }

                    if (!recordDate.before(startDate) && !recordDate.after(endDate)) {
                        String[] employeeInfo = employeeInfoMap.get(employeeNumber);
                        if (employeeInfo != null) {
                            String firstName = employeeInfo[0];
                            String lastName = employeeInfo[1];
                            String birthday = employeeInfo[2];

                            try {
                                long workedMinutes = calculateWorkedHours(timeFormat, logIn, logOut);
                                totalWorkedMinutesMap.put(employeeNumber, totalWorkedMinutesMap.getOrDefault(employeeNumber, 0L) + workedMinutes);
                                found = true;
                            } catch (ParseException e) {
                                System.out.println("Error parsing times for " + firstName + " " + lastName);
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            if (!found) {
                System.out.println("No matching record found for Employee #: " + searchEmployeeNumber);
            }

            if (totalWorkedMinutesMap.containsKey(searchEmployeeNumber)) {
                long totalMinutes = totalWorkedMinutesMap.get(searchEmployeeNumber);
                long totalHours = totalMinutes / 60;
                long remainingMinutes = totalMinutes % 60;

                String[] employeeInfo = employeeInfoMap.get(searchEmployeeNumber);
                String firstName = employeeInfo[0];
                String lastName = employeeInfo[1];
                String birthday = employeeInfo[2];
                String basicSalaryStr = employeeInfo[3];

                double basicSalary = Double.parseDouble(basicSalaryStr);
                double sssContribution = calculateSSSContribution(basicSalary);
                double philHealthContribution = calculatePhilHealthContribution(basicSalary);
                double pagIBIGContribution = calculatePagIBIGContribution(basicSalary);

                double withholdingTax = calculateWithholdingTax(basicSalary, sssContribution, philHealthContribution, pagIBIGContribution);

                System.out.println("\n--- Employee Summary ---");
                System.out.println("Employee #: " + searchEmployeeNumber);
                System.out.println("Name: " + firstName + " " + lastName);
                System.out.println("Birthday: " + birthday);
                System.out.println("Basic Salary: PHP " + basicSalary);
                System.out.println("SSS Contribution: PHP " + sssContribution);
                System.out.println("PhilHealth Contribution (Employee Share): PHP " + philHealthContribution);
                System.out.println("Pag-IBIG Contribution (Employee Share): PHP " + pagIBIGContribution);
                System.out.println("Withholding Tax: PHP " + withholdingTax);
                System.out.println("Total Hours Worked from " + startDateString + " to " + dateFormat.format(endDate) + ": " + totalHours + " hours and " + remainingMinutes + " minutes.");
            } else {
                System.out.println("No work records found for the employee in the specified date range.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    public static long calculateWorkedHours(SimpleDateFormat timeFormat, String logIn, String logOut) throws ParseException {
        long logInTime = timeFormat.parse(logIn).getTime();
        long logOutTime = timeFormat.parse(logOut).getTime();
        long diffMillis = logOutTime - logInTime;
        return diffMillis / (60 * 1000);
    }

    public static double calculateSSSContribution(double basicSalary) {
        if (basicSalary >= 22250 && basicSalary <= 22750) {
            return 1012.50;
        } else if (basicSalary > 22750 && basicSalary <= 23250) {
            return 1035.00;
        } else if (basicSalary > 23250 && basicSalary <= 23750) {
            return 1057.50;
        } else if (basicSalary > 23750 && basicSalary <= 24250) {
            return 1080.00;
        } else if (basicSalary > 24250 && basicSalary <= 24750) {
            return 1102.50;
        } else if (basicSalary > 24750) {
            return 1125.00;
        } else {
            return 0.0;
        }
    }

    public static double calculatePhilHealthContribution(double basicSalary) {
        double premiumRate = 0.03;
        double monthlyPremium = basicSalary * premiumRate;

        if (monthlyPremium < 300) {
            monthlyPremium = 300;
        } else if (monthlyPremium > 1800) {
            monthlyPremium = 1800;
        }

        return monthlyPremium / 2;
    }

    public static double calculatePagIBIGContribution(double basicSalary) {
        double employeeContribution = 0.0;

        if (basicSalary > 1500) {
            employeeContribution = basicSalary * 0.02;
            if (employeeContribution > 100) {
                employeeContribution = 100;
            }
        }

        return employeeContribution;
    }

    public static double calculateWithholdingTax(double basicSalary, double sssDeduction, double philHealthDeduction, double pagIBIGDeduction) {
        double totalDeductions = sssDeduction + philHealthDeduction + pagIBIGDeduction;
        double taxableIncome = basicSalary - totalDeductions;

        double withholdingTax = 0.0;

        if (taxableIncome <= 20832) {
            withholdingTax = 0.0;
        } else if (taxableIncome <= 33333) {
            withholdingTax = (taxableIncome - 20833) * 0.20;
        } else if (taxableIncome <= 66667) {
            withholdingTax = 2500 + (taxableIncome - 33333) * 0.25;
        } else if (taxableIncome <= 166667) {
            withholdingTax = 10833 + (taxableIncome - 66667) * 0.30;
        } else if (taxableIncome <= 666667) {
            withholdingTax = 40833.33 + (taxableIncome - 166667) * 0.32;
        } else {
            withholdingTax = 200833.33 + (taxableIncome - 666667) * 0.35;
        }

        return withholdingTax;
    }
}
