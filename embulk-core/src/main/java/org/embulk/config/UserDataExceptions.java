package org.embulk.config;

public class UserDataExceptions
{
    private UserDataExceptions() { }

    public boolean isUserDataException(Throwable exception)
    {
        while (exception != null) {
            if (exception instanceof UserDataException) {
                return true;
            }
            exception = exception.getCause();
        }
        return false;
    }
}
