package togos.solidtree.trace.job;

public class PerformanceCounter
{
	public volatile long sampleCount;
	public volatile long beginTime;
	public volatile long time;
	public volatile double samplesPerSecondWeightedAverage;
	public volatile double samplesPerSecondLatest;
	
	public void samplesCompleted( long count ) {
		long currentTime = System.currentTimeMillis();
		long interval = currentTime - time;
		synchronized(this) {
			if( beginTime == -1 ) {
				beginTime = currentTime;
			}
			sampleCount += count;
			if( interval > 0 ) {
				samplesPerSecondLatest = count * 1000 / interval;
				samplesPerSecondWeightedAverage =
					(samplesPerSecondWeightedAverage * 0.8) +
					(samplesPerSecondLatest * 0.2);
			}
			time = currentTime;
		}
	}
	
	public String toString() {
		return String.format("% 15d samples total ; % 10.2f samples/second", sampleCount, samplesPerSecondWeightedAverage );
	}
}
