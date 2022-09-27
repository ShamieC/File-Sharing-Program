import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * A GUI used to select the folder with files to be shared.
 *
 */
public class FolderChooser {

	FolderChooser() {
		createWindow();
	}

	private static void createWindow() {    
		JFrame frame = new JFrame(Client.name + ":Share Folder Chooser");
	
		createUI(frame);
		frame.setSize(560, 200);      
		frame.setLocationRelativeTo(null);  
		frame.setVisible(true);
	}

	private static void createUI(final JFrame frame){  
		JPanel panel = new JPanel();
		LayoutManager layout = new FlowLayout();  
		panel.setLayout(layout);       

		JButton button = new JButton("Select the directory with the files you want to share");
		final JLabel label = new JLabel();

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int option = fileChooser.showOpenDialog(frame);
				if(option == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					Client.sharedFolder = file.getAbsolutePath();
					label.setText("Folder Selected: " + file.getName());

				}else{
					label.setText("CANCELLED");
				}
			}
		});

		panel.add(button);
		panel.add(label);
		frame.getContentPane().add(panel, BorderLayout.CENTER);    
	}  
}