RapidJDBC
==========

RapidJDBC is Java library which provides an easier and faster way of developing applications that need to invoke many complex SQL statements. It combines the power of JDBC API and AspectJ features to hide the boiler plate code from the developers. Because of **AspectJ** weaver usage you **must enable weaving** in your application to be able to get RapidJDBC work. To enable weaving you must use a java agent. For example, to run a standalone application execute command:
`java -jar your-application.jar -javaagent:path/to/aspectjweaver.jar`

More information about AspectJ weaving methods can be found [here](http://www.eclipse.org/aspectj/doc/next/devguide/ltw-configuration.html#enabling-load-time-weaving)

Core features:
 - supports all SQLs supported by JDBC (queries, inserts, updates, deletes, alters)
 - developers must deal only with `ResultSet`
 - convenient way to create entities from `ResultSet` using annotation
 - passing SQL statements via annotations
 - transaction support via annotation
 - **thread safe**
 - runs on JDK 1.5 or higher

## Usage
Below are the examples of RapidJDBC usage.
### Creating entity classes
Any class with **no-arg constructor** can be an entity. To map a field to a `ResultSet` column name use `@Column` annotation directly on field. You can also have non-mapped fields in an entity if you want. Remember that you get a benefit from mapped fields only when using `Repository.createEntity(...)`. See section "Executing SQLs" for more details.
```java
import com.github.kopylec.rapidjdbc.annotation.Column;
...

public class Country {

    @Column("id") //Will be mapped to "id" column from result set row.
    private int id;

    @Column("name") //Will be mapped to "name" column from result set row.
    private String name;

    //Won't be mapped to result set column.
    private Date independenceDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getIndependenceDate() {
        return independenceDate;
    }

    public void setIndependenceDate(Date independenceDate) {
        this.independenceDate = independenceDate;
    }
}
```
### Setting up RapidJDBC
First thing you need to do is to **configure** RapidJDBC via `JDBContext` class.
You must set two things:
 - data source
 - entity classes (optionaly)

```java
import com.github.kopylec.rapidjdbc.JDBCContext;
...

DataSource dataSource = new ... //Create a data source

JDBCContext.getInstance().setDataSource(dataSource);
JDBCContext.getInstance().setEntityClasses(Country.class, Person.class);
```
### Executing SQLs
In RapidJDBC you run SQLs by creating **repositories**. To create one you need extend from the basic `Repository` class provided by the library. Every repository **method can execute one and only one SQL** statement. To provide SQL statement parameters use method arguments.

To execute a query you must annotate a repository method with `@Query` annotation. Inside the method you only have to deal with the `ResultSet`. `Repository` class provides convenient methods to simplify working with `ResultSet`. For example you can create an entity with a single `Repository.createEntity(...)` method invocation. To create an entity with this method you must map entity fields to `ResultSet` columns using `@Column` annotation. Remember that creating entity via `Repository.createEntity(...)` is easier but has slightly more performance overhead than using standard result set access methods like `Repository.getString(...)`

To execute an insert, update, delete or alter statement you must annotate a repository method with `@Update` annotation. You don't need to write any code in update methods.
```java
import com.github.kopylec.rapidjdbc.Repository;
import com.github.kopylec.rapidjdbc.annotation.Query;
import com.github.kopylec.rapidjdbc.annotation.Update;
...

public class CountryRepository extends Repository {

    @Query("select * from country where name = ?")
    public List<Country> findCountriesByName(String name) {
        List<Country> countries = new ArrayList<>();
        while (nextRow()) {
            Country country = new Country();
            //Using standard result set access methods.
            country.setId(getInteger("id"));
            country.setName(getString("name"));
            countries.add(country);
        }
        return countries;
    }

    @Query("select * from country")
    public List<Country> findCountries() {
        List<Country> countries = new ArrayList<>();
        while (nextRow()) {
            //Creating an entity.
            countries.add(createEntity(Country.class));
        }
        return countries;
    }

    @Update("update country set name = ? where id = ?")
    public void updateCountryName(String name, int id) {
        //No logic needed here.
    }
}
```
### Transactions
RapidJDBC provides a JDBC transaction support. To run a method in transaction all you need to do is annotate it with `@Transaction` annotation. **Nested transactional** method will be **merged** into single transaction.
```java
import com.github.kopylec.rapidjdbc.JDBCContext;
import com.github.kopylec.rapidjdbc.annotation.Transaction;
...

public class CountryService {

    private final CountryRepository countryRepository = new CountryRepository();

    @Transaction
    public void runInTransaction() {
        List<Country> countriesByName = countryRepository.findCountriesByName("Poland");
        List<Country> countries = countryRepository.findCountries();
        countryRepository.updateCountryName("Poland", 69);
    }
}
```
### Logging
RapidJDBC uses a popular Java logging API called [SLF4J](http://www.slf4j.org/) to log informations. The logs are set to **DEBUG** and **TRACE** levels, so no INFOs will be logged by RapidJDBC.
### Summary
RapidJDBC is primely useful when you have to deal with very complex SQL statements like nested selects etc. The library is thread safe so it can be used in **any Java environment**, for example in standalone Java application or J2EE container. Because of relying only on AspectJ it can be used with any Java framework as a support data access layer.

#### Any feedback will be appreciated.
