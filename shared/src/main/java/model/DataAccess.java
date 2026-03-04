package dataAccess;

import exception.ResponseException;

interface DataAccess {
  // A method for clearing all data from the database. Used during testing.
  public void clear() throws ResponseException;
}
