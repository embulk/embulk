package org.embulk.spi;

// TODO このクラスはembulk-spiにあるべき
// embulk-spiに置いた状態でembulk本体をビルドすることが出来なかったので、仕方なく暫定的にembulk-coreに置いた
/**
 * Embulk plugin factory.
 * 
 * @since FIXME since
 */
public interface EmbulkPluginFactory {

    /**
     * Returns the name of the Embulk plugin.
     * 
     * @return the name of the Embulk plugin
     */
    String getName();

    /**
     * Returns the version of the Embulk plugin.
     * 
     * @return the version of the Embulk plugin
     */
    String getVersion();

    /**
     * Returns the class of the Embulk plugin.
     * 
     * @return the class of the Embulk plugin
     */
    Class<?> getPluginClass();
}
