/*
 * This file may be part of "The Java Telnet Application".
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * "The Java Telnet Application" is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package de.mud.bsx;
/**
 * a LRU Cache for BSX Objects.
 @author  Thomas Kriegelstein
 @version 1.0
 */
public class BSXCache
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
		else
			{
				ids[cacheSize-size]=null;
				bsx[cacheSize-size]=null;
			}
		size--;
    }
    private void move(int offset)
    {
		if (offset>size)
			return;
		// from - to
		for (int i=1;i<=offset;i++)
			{
				bsx[cacheSize-i].flush();
				bsx[cacheSize-i]=null;
				ids[cacheSize-i]=null;
			}
		System.arraycopy(ids,cacheSize-size,
						 ids,cacheSize-size+offset,cacheSize-offset);
		System.arraycopy(bsx,cacheSize-size,
						 bsx,cacheSize-size+offset,cacheSize-offset);
		size-=offset;
    }
    private int find(String id)
    {
		int res=NOT_FOUND;
		for (int index=cacheSize-size;index<cacheSize;index++)
			if (id.equals(ids[index])) 
				{ 
					res=index;
					break;
				}
		return res;
    }
}
