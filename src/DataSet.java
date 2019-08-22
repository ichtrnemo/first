import java.util.ArrayList;

public class DataSet {
    private ArrayList<String> kpp;
    private ArrayList<String> accountNumber;
    private ArrayList<String> currency;

    public DataSet(){};

    public DataSet(ArrayList<String> kpp, ArrayList<String> accountNumber, ArrayList<String> currency){
        this.setKpp(kpp);
        this.setAccountNumber(accountNumber);
        this.setCurrency(currency);
    }

    public ArrayList<String> getKpp() {
        return kpp;
    }

    public void setKpp(ArrayList<String> kpp) {
        this.kpp = kpp;
    }

    public ArrayList<String> getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(ArrayList<String> accountNumber) {
        this.accountNumber = accountNumber;
    }

    public ArrayList<String> getCurrency() {
        return currency;
    }

    public void setCurrency(ArrayList<String> currency) {
        this.currency = currency;
    }
}
