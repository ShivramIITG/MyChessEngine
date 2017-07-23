import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public class MyEngineUI extends JPanel implements MouseListener, MouseMotionListener{
    static int mouseX, mouseY, newMouseX, newMouseY;
    static int squareSize=110;
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.setBackground(Color.yellow);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        for (int i=0;i<64;i+=2) {
            g.setColor(new Color(255,200,100));
            g.fillRect((i%8+(i/8)%2)*squareSize, (i/8)*squareSize, squareSize, squareSize);
            g.setColor(new Color(150,50,30));
            g.fillRect(((i+1)%8-((i+1)/8)%2)*squareSize, ((i+1)/8)*squareSize, squareSize, squareSize);
        }
        Image chessPiecesImage;
        chessPiecesImage=new ImageIcon("ChessPieces.png").getImage();
		
		//knights
		if(MyEngine.whiteKnight1pos!=-1){
			int j=4,k=0,i=63-MyEngine.whiteKnight1pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}

		if(MyEngine.whiteKnight2pos!=-1){
			int j=4,k=0,i=63-MyEngine.whiteKnight2pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		//Rooks
		if(MyEngine.whiteRook1pos!=-1){
			int j=2,k=0,i=63-MyEngine.whiteRook1pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}

		if(MyEngine.whiteRook2pos!=-1){
			int j=2,k=0,i=63-MyEngine.whiteRook2pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		//Bishops
		if(MyEngine.whiteBishop1pos!=-1){
			int j=3,k=0,i=63-MyEngine.whiteBishop1pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}

		if(MyEngine.whiteBishop2pos!=-1){
			int j=3,k=0,i=63-MyEngine.whiteBishop2pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		///Queen
		if(MyEngine.whiteQueenpos!=-1){
			int j=1,k=0,i=63-MyEngine.whiteQueenpos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		//king
		if(MyEngine.whiteKingpos!=-1){
			int j=0,k=0,i=63-MyEngine.whiteKingpos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		//pawns
		for(int var=63;var>=0;var--) {
			int i=63-var;
			if( (MyEngine.whitePawns & (1L<<var)) > 0L) {
				int j=5,k=0;
				g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
			}
			if( (MyEngine.blackPawns & (1L<<var)) > 0L) {
				int j=5,k=1;
				g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
			}
		}
		//knights
		if(MyEngine.blackKnight1pos!=-1){
			int j=4,k=1,i=63-MyEngine.blackKnight1pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}

		if(MyEngine.blackKnight2pos!=-1){
			int j=4,k=1,i=63-MyEngine.blackKnight2pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		//Rooks
		if(MyEngine.blackRook1pos!=-1){
			int j=2,k=1,i=63-MyEngine.blackRook1pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}

		if(MyEngine.blackRook2pos!=-1){
			int j=2,k=1,i=63-MyEngine.blackRook2pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		//Bishops
		if(MyEngine.blackBishop1pos!=-1){
			int j=3,k=1,i=63-MyEngine.blackBishop1pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}

		if(MyEngine.blackBishop2pos!=-1){
			int j=3,k=1,i=63-MyEngine.blackBishop2pos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		///Queen
		if(MyEngine.blackQueenpos!=-1){
			int j=1,k=1,i=63-MyEngine.blackQueenpos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
		//king
		if(MyEngine.blackKingpos!=-1){
			int j=0,k=1,i=63-MyEngine.blackKingpos;
		    g.drawImage(chessPiecesImage, (i%8)*squareSize, (i/8)*squareSize, (i%8+1)*squareSize, (i/8+1)*squareSize, j*64, k*64, (j+1)*64, (k+1)*64, this);	
		}
		
        
    }
    @Override
    public void mouseMoved(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getX()<8*squareSize &&e.getY()<8*squareSize) {
            //if inside the board
            mouseX=e.getX();
            mouseY=e.getY();
            repaint();
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getX()<8*squareSize &&e.getY()<8*squareSize) {
            //if inside the board
            newMouseX=e.getX();
            newMouseY=e.getY();
            if (e.getButton()==MouseEvent.BUTTON1) {
                int xs = mouseY/squareSize, ys = mouseX/squareSize;
				int startPosFind = 63 - xs*8 - ys;
				//long startPos = 1L<<startPosFind;
				
				int xe = newMouseY/squareSize, ye = newMouseX/squareSize;
				int endPosFind = 63 - xe*8 - ye;
				//long endPos = 1L<<endPosFind;
				
				//assuming i dont care abt stalemates, no need to do this
				String whiteMoveAllowed = MyEngine.whiteCheckMoves();
				if(whiteMoveAllowed == ""){
					if(MyEngine.whiteKingSafe() == false){
						JOptionPane.showMessageDialog(this,
							"Game over. U lost",
							"A plain message",
							JOptionPane.PLAIN_MESSAGE);
					}
					else{
						JOptionPane.showMessageDialog(this,
						"Game over. Stalemate",
						"A plain message",
						JOptionPane.PLAIN_MESSAGE);					
					}
				}
				
				//System.out.println(startPosFind + " " + endPosFind);
				boolean whiteMoveCorrect = MyEngine.whitePlaysMove(startPosFind,endPosFind);
				if(whiteMoveCorrect == true){
					//System plays
					String blackMovesAllowed = MyEngine.blackCheckMoves();
					//System.out.println(blackMovesAllowed);
					
					if(blackMovesAllowed == ""){
						if(MyEngine.blackKingSafe() == false){
							JOptionPane.showMessageDialog(this,
								"Game over. U Won",
								"A plain message",
								JOptionPane.PLAIN_MESSAGE);
						}
						else{
							JOptionPane.showMessageDialog(this,
							"Game over. Stalemate",
							"A plain message",
							JOptionPane.PLAIN_MESSAGE);					
						}
					}
					MyEngine.blackPlaysMove();//blackMovesAllowed);
					repaint();
				}
				
            }
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
}