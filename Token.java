public class Token
{
  private String kind;
  private String details;

  public Token( String k, String d )
  {
    kind = k;  details = d;
  }

  public boolean isKind( String s )
  {
    return kind.equals( s );
  }

  public String getKind()
  {  return kind;  }

  public String getDetails()
  { return details; }

  public boolean matches( String k, String d ) {
    return kind.equals(k) && details.equals(d);
  }

  public String toString()
  {
    return "[" + kind + "," + details + "]";
  }

}
