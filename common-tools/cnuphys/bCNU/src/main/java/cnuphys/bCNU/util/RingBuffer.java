package cnuphys.bCNU.util;

import java.util.Vector;

/**
 * A standard FIFO ring buffer
 * @author davidheddle
 *
 * @param <T>
 */
public class RingBuffer<T> extends Vector<T> {
	
	//capacity of the buffer
	private int _capacity;
	
	//index of oldest entry
	public int oldestIndex = -1;
	
	//index of the current entry
	public int currentIndex;

	/**
	 * Create a RingBuffer
	 * @param capacity the capacity of the buffer
	 */
	public RingBuffer(int capacity) {
		super(capacity);
		_capacity = capacity;
		reset();
	}
	
	/**
	 * Clear the data and reset.
	 */
	@Override
	public void clear() {
		super.clear();
		reset();
	}
	
	private void reset() {
		currentIndex = 0;
		oldestIndex = -1;		
	}
	
	@Override
	public boolean add(T elem) {
		
		//don't allow nulls
		if (elem == null) {
			return false;
		}
		
		
		//this will add, placing the element in the 0 position, unless
		//we are full, in which case it replaces at the 0 position
		
		if (isFull()) {
			removeElementAt(oldestIndex);
			insertElementAt(elem, oldestIndex);
			currentIndex = oldestIndex;
			oldestIndex--;
			
			if (oldestIndex < 0) {
				oldestIndex = _capacity-1;
			}
		}
		else {
			insertElementAt(elem, 0);
			currentIndex = 0;
			oldestIndex = (oldestIndex + 1) % _capacity;
		}
		
		return true;
	}
	
	/**
	 * Is the buffer full?
	 * @return <code>true</code> if the buffer is full.
	 */
	public boolean isFull() {
		return size() == _capacity;
	}
	
	/**
	 * get the previous element. This would be the next older element,
	 * modulo the oldest, at which point you'd get the newest (which may not be the current) 
	 * until you get back to yourself.
	 * @return
	 */
	public T previous() {
		if (size() == 0) {
			return null;
		}
		
		if (size() == 1) {
			return firstElement();
		}
		
		currentIndex = (currentIndex + 1) % size();
		return get(currentIndex);
	}
}
