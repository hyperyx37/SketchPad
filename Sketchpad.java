import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Sketchpad extends Frame implements
        ActionListener,             // menu
        ItemListener,               // button
        MouseListener,              // Mouse downclick and upclick
        MouseMotionListener,        // dragging mouse
        WindowListener {            // window open and close
    class obj {
        int x1,x2,y1,y2;
        String Mode;
        String colormode;
        Point[] arr = new Point[10000];
        int index;
        boolean moved;
        obj(int a, int b, int c, int d, String t, Point[] ar,int i,String Co,boolean m) {
            x1 = a;
            y1 = b;
            x2 = c;
            y2 = d;
            Mode = t;
            arr = ar;
            index = i;
            colormode = Co;
            moved = m;
        }
        class ipair {
            int x,y;
            ipair(int xx, int yy) { x=xx; y=yy; }
        }
        ipair  add(ipair U, ipair W) { return new ipair(U.x+W.x, U.y+W.y); }
        ipair  sub(ipair U, ipair W) { return new ipair(U.x-W.x, U.y-W.y); }
        ipair scale(ipair U, float s) {
            return new ipair((int)(s*(float)U.x), (int)(s*(float)U.y)); }
        int dist(ipair P, ipair Q) {
            return (int)Math.sqrt((P.x-Q.x)*(P.x-Q.x) + (P.y-Q.y)*(P.y-Q.y)); }
        int  dot(ipair P, ipair Q) { return P.x*Q.x + P.y*Q.y; }
        int segdist(int xp,int yp) { // distance from (xp,yp) to line (xi,yi,xj,yj)
            ipair I=new ipair(x1,y1), J=new ipair(x2,y2), P=new ipair(xp,yp), N;
            ipair V = sub(J,I);             // V is the vector from I to J
            int k = dot(V, sub(P,I)); // k is the non-normalized projection from P-I to V
            int V2= dot(V,V);         // V2 is the length of V, squared
            if (k<=0) N = I;          // if the projection is negative, I is nearest (N)
            else if (k>=V2) N = J;   // if the projection too large, J is nearest (N)
            else N = add(I, scale(V,(float)k/(float)V2)); //otherwise scale N to V by k/V2
            return dist(P,N);
        }

    }
    // use data structure Arraylist to store each sketch
    ArrayList<obj> SketchData = new ArrayList<>();
    ArrayList<obj> Save = new ArrayList<>(); // another obj ArrayList to store data
    ArrayList<obj> SketchData2 = new ArrayList<>(); // obj ArrayList to store original obj for move and cut
    ArrayList<Integer> locations = new ArrayList<>(); // int list to store obj original locations

    int shift = 10;
    // use Point array to draw free lines and erase
    Point[] a = new Point[10000];
    int i = 0; // index of point array

    // Select,move,cut,copy and paste mode
    boolean SelectionMode = false;
    boolean MoveMode = false;

    obj Cobj; // Cutted or Copied object
    int Cloc; // Cutted or Copied object location

    //point for selected object
    int point;

    // Coordinates Distances between selected point and object points
    int dx1,dx2,dy1,dy2;
    Point[] da = new Point[10000];

    // window size
    static final int width = 800;
    static final int height = 600;

    // initial coordinates
    int X0,Y0;
    int X1,X2,Y1,Y2;
    // size of the rectangular
    int w,h;

    // chosen tool mode and color mode
    String OptionSelected = "freeDraw";
    String ColorSelected = "Black";

    // Color and tool menu
    String[] Toolbox = {"freeDraw","line","square","rectangle","circle","ellipse","Eraser","polygon"};
    String[] Colorbox = {"Black","Cyan","Green","Yellow","Magenta","Red","Blue"};

    void MenuInit() {
        MenuBar B = new MenuBar();
        // Add tools menu
        Menu tools = new Menu("Shapes");
        for (int i = 0; i< Toolbox.length; i++) {
            tools.add(Toolbox[i]);
        }
        B.add(tools);
        tools.addActionListener(this);

        Menu colors = new Menu("Colors");
        for (int j=0;j<Colorbox.length;j++) {
            colors.add(Colorbox[j]);
        }
        B.add(colors);
        colors.addActionListener(this);
        setMenuBar(B);
    }

    public Sketchpad(String title) {
        super(title);
        addMouseMotionListener(this);
        addWindowListener(this);
        addMouseListener(this);
        setLayout(new FlowLayout());
        MenuInit();
        setBackground(Color.white);
        Button btn1 = new Button("Undo");
        add(btn1);
        btn1.addActionListener(e -> {
            if (SketchData.size()!=0) {
                if (SketchData.get(SketchData.size()-1).moved==true || SketchData.get(SketchData.size()-1).Mode.equals("cut")) {
                    SketchData.remove(SketchData.size()-1);
                    SketchData.add(locations.get(locations.size()-1),SketchData2.get(SketchData2.size()-1));
                    locations.remove(locations.size()-1);
                    SketchData2.remove(SketchData2.size()-1);
                } else {
                    SketchData.remove(SketchData.size()-1);
                }
            }
            repaint();
        });

        Button btn2 = new Button("ClearCanvas");
        add(btn2);
        btn2.addActionListener(e -> {
            SketchData.clear();
            i=0;
            SelectionMode = false;
            MoveMode = false;
            repaint();
        });

        Button btn3 = new Button("Save");
        add(btn3);
        btn3.addActionListener(e -> {
            Save.clear();
            Save = new ArrayList<>(SketchData);
        });

        Button btn4 = new Button("Load");
        add(btn4);
        btn4.addActionListener(e -> {
            SketchData.clear();
            SketchData = new ArrayList<>(Save);
            repaint();
        });

        Button  btn5 = new Button("Select");
        add(btn5);
        btn5.addActionListener(e -> {
            SelectionMode = true;
            MoveMode = false;
        });

        Button  btn6 = new Button("Move");
        add(btn6);
        btn6.addActionListener(e -> {
            if (SelectionMode == true) {MoveMode = true;}
        });

        Button  btn7 = new Button("CUT");
        add(btn7);
        btn7.addActionListener(e -> {
            if (SelectionMode == true) {
                SelectionMode = false;
                shift = 10;
                Cobj = SketchData.get(point);
                Cloc = point;
                SketchData2.add(SketchData.get(point));
                locations.add(point);
                SketchData.remove(point);
                SketchData.add(new obj(0,0,0,0,"cut",Cobj.arr,0,"cut",false));
                repaint();
            }
        });

        Button  btn8 = new Button("COPY");
        add(btn8);
        btn8.addActionListener(e -> {
            if (SelectionMode == true) {
                SelectionMode = false;
                shift = 10;
                Cobj = SketchData.get(point);
                Cloc = point;
            }
        });

        Button  btn9 = new Button("PASTE");
        add(btn9);
        btn9.addActionListener(e -> {
            if (Cobj!=null) {
                Point[] newa = new Point[10000];
                shift = shift+10;
                for (int t = 0;t<Cobj.index;t++) {
                    newa[t] = new Point(Cobj.arr[t].x+shift,Cobj.arr[t].y+shift);
                }
                SketchData.add(new obj(Cobj.x1+shift,Cobj.y1+shift,Cobj.x2+shift,Cobj.y2+shift,Cobj.Mode,newa,Cobj.index,Cobj.colormode,false));
                repaint();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Graphics g = getGraphics();
        Object s = e.getActionCommand();
        for (int ind = 0; ind< Toolbox.length; ind++) {
            if (s.equals(Toolbox[ind])) {
                OptionSelected = Toolbox[ind];
                SelectionMode = false;
                MoveMode = false;
                return;
            }
        }
        for (int in=0;in< Colorbox.length;in++) {
            if (s.equals(Colorbox[in])) {
                ColorSelected = Colorbox[in];
                return;
            }
        }

    }

    @Override
    public void itemStateChanged(ItemEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        X0 = 0;
        Y0 = 0;
        w = 0;
        h = 0;
        if (SelectionMode == true && MoveMode == false) {
            X1 = e.getX();
            Y1 = e.getY();
            a = new Point[10000];
            point = ClosestObj(X1, Y1);
            dx1 = X1-SketchData.get(point).x1;
            dx2 = X1-SketchData.get(point).x2;
            dy1 = Y1-SketchData.get(point).y1;
            dy2 = Y1-SketchData.get(point).y2;
            da = new Point[10000];
            for (int index = 0;index<SketchData.get(point).index;index++) {
                int nx = X1-SketchData.get(point).arr[index].x;
                int ny = Y1-SketchData.get(point).arr[index].y;
                da[index] = new Point(nx,ny);
            }

        } else if(MoveMode == true && SelectionMode == true) {
            X1 = e.getX();
            Y1 = e.getY();
            SketchData2.add(SketchData.get(point));
            locations.add(point);
            SketchData.remove(point);
            for(int in = 0;in<SketchData2.get(SketchData2.size()-1).index;in++) {
                a[in] = new Point(X1-da[in].x,Y1-da[in].y);
            }
            SketchData.add(new obj(X1+dx1,Y1+dy1,X1+dx2,Y1+dy2,SketchData2.get(SketchData2.size()-1).Mode,a,SketchData2.get(SketchData2.size()-1).index,SketchData2.get(SketchData2.size()-1).colormode,true));
        } else {
            if (OptionSelected.equals("polygon")) {
                if (i == 0) {
                    X1 = e.getX();
                    Y1 = e.getY();
                    a = new Point[10000];
                    a[i] = new Point(X1, Y1);
                    SketchData.add(new obj(X1, Y1, X1, Y1, OptionSelected, a, i, ColorSelected,false));
                } else {
                    X1 = X2;
                    Y1 = Y2;
                    a[i] = new Point(X1, Y1);

                }

            } else {
                i = 0;
                a = new Point[10000];
                X1 = e.getX();
                Y1 = e.getY();
                a[i] = new Point(X1, Y1);
                SketchData.add(new obj(X1, Y1, X1, Y1, OptionSelected, a, i, ColorSelected,false));
            }
            i++;
        }
    }

        int ClosestObj(int x,int y) {
            int min = SketchData.get(0).segdist(x,y);
            int loc = 0;
            for (int i=0;i<SketchData.size();i++) {
                if (SketchData.get(i).segdist(x,y)<min) {
                    min = SketchData.get(i).segdist(x,y);
                    loc = i;
                }
            }
            return loc;
        }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (MoveMode == true) {
            MoveMode = false;
            SelectionMode = false;
        } else {
            if (OptionSelected.equals("polygon")) {}
            else {i=0;}
        }

    }



    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Graphics g = getGraphics();
        X2 = e.getX();
        Y2 = e.getY();
        if (MoveMode == true) {
            a = new Point[10000];
            for(int index = 0;index<SketchData2.get(SketchData2.size()-1).index;index++) {
                a[index] = new Point(X2-da[index].x,Y2-da[index].y);
            }
            SketchData.remove(SketchData.size()-1);
            SketchData.add(new obj(X2+dx1,Y2+dy1,X2+dx2,Y2+dy2,SketchData2.get(SketchData2.size()-1).Mode,a,SketchData2.get(SketchData2.size()-1).index,SketchData2.get(SketchData2.size()-1).colormode,true));
            repaint();
        } else {
            if (OptionSelected.equals("polygon")) {
                a[i] = new Point(X2,Y2);

                SketchData.remove(SketchData.size()-1);

                SketchData.add(new obj(X1,Y1,X2,Y2,OptionSelected,a,i,ColorSelected,false));
            } else {
                a[i] = new Point(X2,Y2);
                SketchData.remove(SketchData.size()-1);
                SketchData.add(new obj(X1,Y1,X2,Y2,OptionSelected,a,i,ColorSelected,false));
                i++;
            }
            repaint();
        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    void DrawShapeSelected(Graphics g, int A, int B, int C, int D, String shapeSelection, Point[] Ary,int I,String colorM) {
        X0 = Math.min(A,C);
        Y0 = Math.min(B,D);
        w = Math.abs(A-C);
        h = Math.abs(B-D);
        if (colorM.equals("Black")) {
            g.setColor(Color.black);
        } else if (colorM.equals("Cyan")) {
            g.setColor(Color.cyan);
        } else if (colorM.equals("Green")) {
            g.setColor(Color.green);
        } else if (colorM.equals("Magenta")) {
            g.setColor(Color.magenta);
        } else if (colorM.equals("Red")) {
            g.setColor(Color.red);
        } else if (colorM.equals("Blue")) {
            g.setColor(Color.blue);
        } else if (colorM.equals("Yellow")) {
            g.setColor(Color.yellow);
        }
        // Draw selected shape
        if (shapeSelection.equals("square")) {
            g.drawRect(X0,Y0,w,w);
        } else if (shapeSelection.equals("rectangle")) {
            g.drawRect(X0,Y0,w,h);
        } else if (shapeSelection.equals("circle")) {
            g.drawOval(X0,Y0,w,w);
        } else if (shapeSelection.equals("ellipse")) {
            g.drawOval(X0,Y0,w,h);
        } else if (shapeSelection.equals("line")) {
            //ColorOption(g,color);
            g.drawLine(A,B,C,D);
        } else if (shapeSelection.equals("freeDraw")) {
            for (int j = 0;j<I-1;j++) {
                g.drawLine(Ary[j].x,Ary[j].y,Ary[j+1].x,Ary[j+1].y);
            }
        } else if (shapeSelection.equals("Eraser")) {
            for (int k = 0;k<I;k++) {
                g.setColor(Color.white);
                g.fillOval(Ary[k].x,Ary[k].y,10,10);
            }
        } else if (shapeSelection.equals("polygon")) {
            for (int m=0;m<I-1;m++) {
                g.drawLine(Ary[I-m-1].x,Ary[I-m-1].y,Ary[I-m].x,Ary[I-m].y);
            }
        }
    }
    public void paint(Graphics g) {
        SketchData.forEach(l->DrawShapeSelected(g,l.x1,l.y1,l.x2,l.y2,l.Mode,l.arr,l.index,l.colormode));
    }
    public static void main(String[] args) {
        Sketchpad screen = new Sketchpad("Sketchpad");
        screen.setSize(Sketchpad.width,Sketchpad.height);
        screen.setVisible(true);
    }
}
