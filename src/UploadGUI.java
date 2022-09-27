
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;

/**
 * 
 * Code for the GUI that pops up when a client is uploading a file.
 *
 */
public class UploadGUI extends JFrame implements ActionListener {

	private javax.swing.JLabel Label1;
	private javax.swing.JLabel Label2;
	public javax.swing.JProgressBar ProgressBar;

	UploadGUI() {

		Label1 = new javax.swing.JLabel();
		ProgressBar = new javax.swing.JProgressBar(0, 100);
		ProgressBar.setStringPainted(true);
		Label2 = new javax.swing.JLabel();

		Label2.setFont(new java.awt.Font("Lucida Grande", 0, 12));
		setTitle(Client.name + ":Uploading File");
	

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
				.createSequentialGroup().addGap(64, 64, 64)
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
						.addComponent(Label1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
						.addComponent(ProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE).addComponent(Label2,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addContainerGap(64, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
				.createSequentialGroup().addGap(65, 65, 65)
				.addComponent(Label1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(Label2).addGap(8, 8, 8)
				.addComponent(ProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addContainerGap(134, Short.MAX_VALUE)));
		pack();
	}

	public void updateProgress(int value, String filename) {
		if (value == 100) {
			Label2.setText(filename + " Uploaded successfully!");
			ProgressBar.setValue(value);
		} else {
			Label2.setText("Uploading " + filename);
			ProgressBar.setValue(value);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {}
}