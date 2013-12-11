package org.apache.http.client.methods;

import java.net.URI;

/**
 * dient dem senden eines DELETE Bodys das mit HttpDelete nicht möglich ist
 * 
 * @author Stephan
 *
 */
public class HttpDeleteEntity extends HttpPost
{
	public final static String METHOD_NAME = "DELETE";
	
	public HttpDeleteEntity()
	{
		super();
	}
	public HttpDeleteEntity(String url)
	{
		super(url);
		setURI(URI.create(url));
	}
    public HttpDeleteEntity(final URI uri) 
    {
        super();
        setURI(uri);
    }
	@Override
	public String getMethod()
	{
		return METHOD_NAME;
	}
}
