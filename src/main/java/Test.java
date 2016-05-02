import uk.co.agware.carpet.MagicCarpet;
import uk.co.agware.carpet.database.DatabaseConnector;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 28/02/2016.
 */
public class Test {

    public static void main(String[] args) {
        DatabaseConnector databaseConnector = new DatabaseConnector();
        databaseConnector.setConnection("jdbc:postgresql://localhost:5432/test", "user", "password");
        MagicCarpet magicCarpet = new MagicCarpet(databaseConnector);
        magicCarpet.run();
    }
}
