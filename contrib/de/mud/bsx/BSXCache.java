package de.mud.bsx;

final class BSXCache
{
    /** the maximum size of cache */
    protected int cacheSize = 40;
    /** the ids of the objects */
    protected String[] ids = new String[cacheSize];
    /** the objects */
    protected BSXObject[] bsx = new BSXObject[cacheSize];
    /** the size of the cache */
    protected int size=0;
    /** illegal index */
    protected final static int NOT_FOUND = -1;

    public int size()
    {
	return size;
    }
    public String toString()
    {
	StringBuffer res=new StringBuffer(""+size+" entries\n");
	for(int i=cacheSize-size;i<cacheSize;i++)
	    res.append("\t\t"+i+":\t"+ids[i]+"\n");
	return res.toString();
    }
    public BSXObject getEntry(String id)
    {
	int index;
	BSXObject res;
	index=find(id);
	if (NOT_FOUND!=index)
	    {
		res=bsx[index];
		if (index!=cacheSize-size)
		    {
			remove(index);
			size++;
			ids[cacheSize-size]=id;
			bsx[cacheSize-size]=res;
		    }
	    }
	else
	    {
		res=null;
	    }
	return res;
    }
    public void removeEntry(String id)
    {
	int index;
	BSXObject obj;
	index=find(id);
	if (NOT_FOUND!=index)
	    {
		obj = bsx[index];
		obj.flush();
		remove(index);
	    }
    }
    public void addEntry(String id,BSXObject bsxobject)
    {
	int index;
	index=find(id);
	if (NOT_FOUND!=index)
	    {
		remove(index);
	    }
	else
	    {
		if (size==cacheSize)
		    move(cacheSize/10);
	    }
	size++;
	ids[cacheSize-size]=id;
	bsx[cacheSize-size]=bsxobject;
    }
    public boolean containsEntry(String id)
    {
	int index;
	index=find(id);
	return (NOT_FOUND!=index);
    }
    private void remove(int index)
    {
	if (index!=cacheSize-size)
	    {
		int len;
		len=index-(cacheSize-size);
		System.arraycopy(ids,cacheSize-size,
				 ids,cacheSize-size+1,len);
		System.arraycopy(bsx,cacheSize-size,
				 bsx,cacheSize-size+1,len);
	    }
	size--;
    }
    private void move(int offset)
    {
	// from - to
	System.arraycopy(ids,cacheSize-size,
			 ids,cacheSize-size+offset,cacheSize-offset-1);
	System.arraycopy(bsx,cacheSize-size,
			 bsx,cacheSize-size+offset,cacheSize-offset-1);
	size-=offset;
    }
    private int find(String id)
    {
	int res=NOT_FOUND;
	for (int index=cacheSize-size;index<cacheSize;index++)
	    if (ids[index].equals(id)) 
		{ res=index;
		  break;
		}
	return res;
    }
}
