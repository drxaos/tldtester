package ru.ihc.tldtester;

import de.malkusch.whoisServerList.publicSuffixList.PublicSuffixList;
import de.malkusch.whoisServerList.publicSuffixList.PublicSuffixListFactory;

import java.io.IOException;
import java.sql.*;

public class Main {

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        PublicSuffixList suffixList = getPublicSuffixList();

        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/panel_dev", "root", "root");

        PreparedStatement preparedStatement = conn.prepareStatement("update _sites set top = ? where id = ?");

        Statement statement = conn.createStatement();
        ResultSet rs = statement.executeQuery("SELECT id, site FROM _sites");

        int n = 0;

        while (rs.next()) {
            Long id = rs.getLong("id");
            String domain = rs.getString("site");

            boolean isTld = checkPublicSuffix(domain, suffixList);

            preparedStatement.setBoolean(1, isTld);
            preparedStatement.setLong(2, id);
            preparedStatement.addBatch();

            if (++n % 1000 == 0) {
                preparedStatement.executeBatch();
                preparedStatement.clearParameters();

                System.out.println(n);
            }
        }
        preparedStatement.executeBatch();

        rs.close();
        statement.close();
        preparedStatement.close();
        conn.close();
    }

    private static boolean checkPublicSuffix(String domain, PublicSuffixList suffixList) {
        String[] split = domain.split("\\.", 2);
        if (split.length != 2) {
            return false;
        }
        return suffixList.isPublicSuffix(split[1]);
    }

    private static PublicSuffixList getPublicSuffixList() throws IOException, ClassNotFoundException {
        PublicSuffixListFactory factory = new PublicSuffixListFactory();
        return factory.download();
    }

}
