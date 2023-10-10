package org.example;

import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>MyPlugin")
public class MyPlugin<T extends RealType<T>> implements Command {

    @Override
    public void run() {

    }
}